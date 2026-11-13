#!/usr/bin/env python3
"""NEXO-Δ: laboratorio contrafactual local, sin conexión al IAM ni a MySQL."""
import argparse
import copy
import hashlib
import itertools
import json
from pathlib import Path
import re
import urllib.request

PERMISSIONS = {"profile:read", "users:read", "users:write"}
WEIGHTS = {"profile:read": 2, "users:read": 15, "users:write": 35}

def exact(obj, keys):
    if not isinstance(obj, dict) or set(obj) != set(keys):
        raise ValueError("Campos inesperados o faltantes; no incluir datos personales ni secretos")

def validate(s):
    exact(s, ["synthetic", "roles", "users", "change", "required_permissions", "at_minute"])
    if s["synthetic"] is not True: raise ValueError("Solo snapshots sintéticos")
    if not isinstance(s["roles"], dict) or not 1 <= len(s["roles"]) <= 8: raise ValueError("Catálogo inválido")
    for code, perms in s["roles"].items():
        if not re.fullmatch(r"[A-Z_]{1,30}", code) or not isinstance(perms, list) or not set(perms) <= PERMISSIONS: raise ValueError("Rol inválido")
    def role_list(value):
        if not isinstance(value, list) or len(value) != len(set(value)) or not set(value) <= s["roles"].keys(): raise ValueError("Roles desconocidos/duplicados")
    if not isinstance(s["users"], list) or not 1 <= len(s["users"]) <= 100: raise ValueError("Usuarios fuera de límite")
    ids = set()
    for u in s["users"]:
        exact(u, ["id", "roles", "status", "session_version", "sessions"])
        if not re.fullmatch(r"u-[0-9]{1,3}", u["id"]) or u["id"] in ids: raise ValueError("Usa identificadores sintéticos u-1, u-2...")
        ids.add(u["id"]); role_list(u["roles"])
        if u["status"] not in ["PENDING", "ACTIVE", "LOCKED", "DISABLED"] or type(u["session_version"]) is not int or u["session_version"] < 0: raise ValueError("Estado inválido")
        if not isinstance(u["sessions"], list) or len(u["sessions"]) > 20: raise ValueError("Sesiones fuera de límite")
        for t in u["sessions"]:
            exact(t, ["kind", "sv", "expires_minute", "consumed"])
            if t["kind"] not in ["access", "refresh"] or type(t["sv"]) is not int or type(t["expires_minute"]) is not int or type(t["consumed"]) is not bool: raise ValueError("Sesión inválida")
    exact(s["change"], ["assignments", "permission_patch"])
    if not isinstance(s["change"]["assignments"], dict) or not set(s["change"]["assignments"]) <= ids: raise ValueError("Destinatario desconocido")
    for roles in s["change"]["assignments"].values():
        role_list(roles)
        if not roles: raise ValueError("assignRoles exige al menos un rol")
    patch = s["change"]["permission_patch"]
    if not isinstance(patch, dict) or not set(patch) <= s["roles"].keys(): raise ValueError("Parche inválido")
    for perms in patch.values():
        if not isinstance(perms, list) or not set(perms) <= PERMISSIONS: raise ValueError("Permisos desconocidos")
    if not isinstance(s["required_permissions"], list) or not set(s["required_permissions"]) <= PERMISSIONS: raise ValueError("Necesidad inválida")
    if type(s["at_minute"]) is not int or not 0 <= s["at_minute"] <= 10080: raise ValueError("Horizonte inválido")
    return s

def effective(roles, catalog):
    return set().union(*(set(catalog[r]) for r in roles)) if roles else set()

def valid_session(t, user, minute):
    return (user["status"] == "ACTIVE" and t["expires_minute"] > minute
            and (t["kind"] != "access" or t["sv"] == user["session_version"])
            and (t["kind"] != "refresh" or not t["consumed"]))

def simulate(snapshot, change=None):
    s = validate(copy.deepcopy(snapshot)); before = copy.deepcopy(s)
    c = change if change is not None else s["change"]
    s["change"] = c; validate(s)
    s["roles"].update(c["permission_patch"])
    affected = []
    for u in s["users"]:
        old = next(v for v in before["users"] if v["id"] == u["id"])
        if u["id"] in c["assignments"]:
            u["roles"] = c["assignments"][u["id"]]
            u["session_version"] += 1  # assignRoles invalida incluso una asignación idéntica.
            for t in u["sessions"]:
                if t["kind"] == "refresh": t["consumed"] = True
        oldp = effective(old["roles"], before["roles"]); newp = effective(u["roles"], s["roles"])
        if oldp != newp or u["id"] in c["assignments"]:
            at = s["at_minute"]
            live = sum(valid_session(t, old, at) for t in old["sessions"])
            after_live = sum(valid_session(t, u, at) for t in u["sessions"])
            affected.append({"user": u["id"], "before_roles": old["roles"], "proposed_roles": u["roles"], "before_permissions": sorted(oldp), "proposed_permissions": sorted(newp), "gained": sorted(newp-oldp), "lost": sorted(oldp-newp),
                             "sessions_valid_at_horizon_before": live,
                             "sessions_invalidated_by_change": live-after_live,
                             "sessions_still_valid": after_live})
    admins = sum(u["status"] == "ACTIVE" and "users:write" in effective(u["roles"], s["roles"]) for u in s["users"])
    gains = sum(WEIGHTS[p] for u in affected for p in u["gained"])
    disruption = min(20, 5 * sum(u["sessions_invalidated_by_change"] for u in affected))
    lockout = 40 if admins == 0 else 0
    score = min(100, gains+disruption+lockout)
    return {"at_minute": s["at_minute"], "affected": affected, "remaining_active_writers": admins,
            "risk": {"score": score, "label": "alto" if score >= 60 else "medio" if score >= 25 else "bajo",
                     "privilege_gain_points": gains, "session_disruption_points": disruption, "no_writer_points": lockout},
            "rollback_proposal_only": {"assignments": {u["id"]: u["roles"] for u in before["users"] if u["id"] in c["assignments"]},
              "permission_patch": {r: before["roles"][r] for r in c["permission_patch"]},
              "warning": "Restaurar roles requiere revisión humana y autenticación nueva; nunca restaurar session_version ni tokens consumidos."}}

def candidates(s):
    required = set(s["required_permissions"]); roles = sorted(s["roles"])
    choices = []
    for n in range(len(roles)+1):
        for subset in itertools.combinations(roles, n):
            perms = effective(subset, s["roles"])
            if required <= perms:
                choices.append({"roles": list(subset), "extra_permissions": sorted(perms-required),
                                "extra_weight": sum(WEIGHTS[p] for p in perms-required)})
    return sorted(choices, key=lambda c: (c["extra_weight"], len(c["roles"]), c["roles"]))[:8]

def explain(report, model):
    # URL literal loopback, sin redirecciones, sin claves y sin herramientas de escritura.
    schema = {"type": "object", "properties": {"explanation": {"type": "string"}, "human_review": {"type": "string"}},
              "required": ["explanation", "human_review"], "additionalProperties": False}
    facts = []
    for u in report["requested"]["affected"]:
        facts.append(f"{u['user']}: ANTES roles={u['before_roles']}, permisos={u['before_permissions']}. "
                     f"DESPUÉS HIPOTÉTICO roles={u['proposed_roles']}, permisos={u['proposed_permissions']}. "
                     f"Permisos que NO tenía antes y ganaría={u['gained']}; perdería={u['lost']}. "
                     f"Tokens invalidados por el cambio={u['sessions_invalidated_by_change']}; siguen válidos={u['sessions_still_valid']}.")
    alternative = report["alternative_evaluated"]
    alt_risk = alternative["simulation"]["risk"]["score"] if alternative else None
    no_change = bool(alternative and alternative["no_change_needed"])
    context = {"project": "ExamenErik", "required_permissions_for_task": report["required_permissions"],
               "requested_change_facts": facts, "requested_change_risk": report["requested"]["risk"],
               "alternative_risk": alt_risk,
               "alternative_action": "CONSERVAR ESTADO ORIGINAL: satisface la tarea SIN los permisos nuevos del cambio solicitado" if no_change else "El catálogo no ofrece una mejora de menor privilegio; posponer el cambio deja pendiente la tarea. Diseñar un rol de lectura sería trabajo futuro",
               "rollback": report["requested"]["rollback_proposal_only"],
               "execution": "Solo simulación: ninguno de los cambios se aplicó"}
    payload = {"model": model, "stream": False, "format": schema,
               "options": {"temperature": 0, "num_predict": 650},
               "messages": [{"role": "system", "content": "Eres NEXO-Δ de ExamenErik. Devuelve JSON con explanation y human_review, dos párrafos breves en español, sin otros campos. Explica primero el CAMBIO SOLICITADO con su riesgo y efectos, después la ALTERNATIVA con su riesgo. Los hechos antes/después son definitivos: un permiso nuevo no existía antes. No confundas el riesgo del cambio con el de conservar el estado original. Respeta alternative_action literalmente en significado. El puntaje es una rúbrica, no probabilidad. Nunca afirmes ejecutar cambios ni revivir sesiones. La recomendación queda para revisión humana. No agregues hechos, roles implementados ni políticas que no consten en los datos."},
                            {"role": "user", "content": json.dumps(context, ensure_ascii=False)}]}
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, *args, **kwargs): return None
    req = urllib.request.Request("http://127.0.0.1:11434/api/chat", json.dumps(payload).encode(), {"Content-Type": "application/json"})
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())
    with opener.open(req, timeout=180) as response:
        result = json.loads(response.read(100000))
    text = json.loads(result["message"]["content"])
    exact(text, ["explanation", "human_review"])
    if not all(isinstance(v, str) and len(v) <= 6000 for v in text.values()): raise ValueError("Salida IA inválida")
    return {"status": "executed", "provider": "Ollama loopback", "model": result["model"], "created_at": result["created_at"],
            "eval_count": result.get("eval_count"), "advisory_untrusted": text}

def run(s, use_ai=False, model="qwen2.5-coder:7b"):
    validate(s)
    requested = simulate(s)
    options = candidates(s)
    alternative = None
    targets = set(s["change"]["assignments"]) | {u["id"] for u in s["users"] if set(u["roles"]) & set(s["change"]["permission_patch"])}
    if options and options[0]["roles"] and targets:
        assignments = {u["id"]: options[0]["roles"] for u in s["users"] if u["id"] in targets and set(u["roles"]) != set(options[0]["roles"])}
        c = {"assignments": assignments, "permission_patch": {}}
        alternative = {"change": c, "no_change_needed": not assignments, "simulation": simulate(s, c)}
    report = {"agent": "NEXO-Δ", "project": "ExamenErik", "mode": "simulation_only",
              "input_sha256": hashlib.sha256(json.dumps(s, sort_keys=True).encode()).hexdigest(),
              "required_permissions": sorted(s["required_permissions"]),
              "requested": requested, "least_privilege_candidates": options,
              "alternative_evaluated": alternative,
              "limitations": ["Snapshot sintético; no predice ataques ni disponibilidad real.",
                "permission_patch es una hipótesis de catálogo: no existe endpoint para editarlo en el IAM.",
                "Estado LOCKED se trata como no activo; no se modela vencimiento del bloqueo.",
                "No modela carreras de concurrencia ni reutilización de refresh consumido (que revoca todas las sesiones).",
                "El puntaje es una rúbrica educativa, no una probabilidad ni certificación."]}
    if use_ai:
        report["ai"] = explain(report, model)
    else: report["ai"] = {"status": "not_requested", "note": "Motor determinista; no afirmar que se ejecutó IA"}
    return report

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("snapshot", type=Path); parser.add_argument("--ai", action="store_true")
    parser.add_argument("--model", default="qwen2.5-coder:7b")
    args = parser.parse_args()
    try:
        if args.snapshot.stat().st_size > 100000: raise ValueError("Snapshot demasiado grande")
        report = run(json.loads(args.snapshot.read_text()), args.ai, args.model)
        print(json.dumps(report, ensure_ascii=False, indent=2))
    except Exception as exc:
        raise SystemExit(f"NEXO-Δ detenido sin cambios: {type(exc).__name__}: {exc}")
