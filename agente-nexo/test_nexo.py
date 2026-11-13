import copy
import json
from pathlib import Path
import unittest
from nexo import run, simulate, validate

class NexoTest(unittest.TestCase):
    def setUp(self): self.s = json.loads((Path(__file__).parent / 'escenario.json').read_text())
    def test_assignment_revokes_access_and_refresh_without_mutating_input(self):
        original = copy.deepcopy(self.s); out = simulate(self.s)
        self.assertEqual(out['affected'][0]['sessions_invalidated_by_change'], 2)
        self.assertEqual(self.s, original)
    def test_expired_sessions_are_not_counted_as_change_damage(self):
        self.s['at_minute'] = 20
        self.assertEqual(simulate(self.s)['affected'][0]['sessions_invalidated_by_change'], 1)
    def test_least_privilege_uses_real_catalog(self):
        report = run(self.s)
        self.assertEqual(report['least_privilege_candidates'][0]['roles'], ['USER'])
        self.assertEqual(report['ai']['status'], 'not_requested')
        self.assertTrue(report['alternative_evaluated']['no_change_needed'])
        self.assertEqual(report['alternative_evaluated']['simulation']['risk']['score'], 0)
    def test_no_readonly_admin_role_is_invented(self):
        self.s['required_permissions'] = ['users:read']
        c = run(self.s)['least_privilege_candidates'][0]
        self.assertEqual(c['roles'], ['ADMIN']); self.assertIn('users:write', c['extra_permissions'])
    def test_last_writer_loss_scores_lockout(self):
        self.s['change']['assignments'] = {'u-1':['USER']}
        self.assertEqual(simulate(self.s)['risk']['no_writer_points'], 40)
    def test_permission_patch_changes_authorities_without_revoking_sessions(self):
        self.s['change'] = {'assignments':{}, 'permission_patch':{'USER':['profile:read','users:write']}}
        self.assertEqual(len(simulate(self.s)['affected']), 2)
        self.assertTrue(all(u['sessions_still_valid'] == 2 for u in simulate(self.s)['affected']))
    def test_rejects_personal_fields_and_unknown_permissions(self):
        self.s['users'][0]['email'] = 'person@example.test'
        with self.assertRaises(ValueError): validate(self.s)
        del self.s['users'][0]['email']; self.s['roles']['USER'].append('execute:shell')
        with self.assertRaises(ValueError): validate(self.s)

if __name__ == '__main__': unittest.main()
