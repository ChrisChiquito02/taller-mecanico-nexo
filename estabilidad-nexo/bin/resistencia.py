import concurrent.futures,json,statistics,subprocess,threading,time
from collections import Counter
from pathlib import Path
from carga import account,call
ROOT=Path(__file__).parent.parent/'evidencias'
SHORT='Calcula 17 por 23. Solo el número.'
LONG='Escribe una explicación detallada de las etapas de un compilador, con ejemplos y un párrafo por etapa.'
def sample():
    return {
        'at':time.monotonic(),
        'processes':subprocess.check_output(['ps','-axo','pid,rss,pcpu,comm'],text=True).splitlines(),
        'mysql':subprocess.check_output(['docker','stats','--no-stream','--format','{{.MemUsage}} {{.CPUPerc}}','examenerik-audit-mysql'],text=True).strip()}
def run():
    accounts=[account() for _ in range(6)]
    chats=[call(8090,'/nexo/conversations',{},t)[1]['id'] for t in accounts]
    started=time.monotonic()
    duration=180
    rows=[]
    samples=[]
    def worker(i):
        token=accounts[i]
        chat=chats[i]
        index=0
        saved=0
        while time.monotonic()-started<duration and saved<40:
            port=8090+(i+index)%2
            long=index%5==4
            tick=time.monotonic()
            try:
                code,data,elapsed=call(port,f'/nexo/conversations/{chat}/messages',{'text':LONG if long else SHORT,'tool':'general'},token,timeout=215)
            except OSError as failure:
                code,data,elapsed=0,{'code':type(failure).__name__},time.monotonic()-tick
            if isinstance(data,list):
                last=data[-1]
                state=last.get('code',last['type'])
                if state=='done':
                    answer=''.join(x['text'] for x in data if x['type']=='delta')
                    if not long and '391' not in answer: state='INCORRECT'
                    saved+=1
            else: state=data.get('code',str(code))
            rows.append(dict(account=i,port=port,long=long,seconds=round(elapsed,3),http=code,state=state))
            index+=1
            time.sleep(1.4)
        code,history,_=call(8090,f'/nexo/conversations/{chat}',token=token)
        return (i,saved,len(history),code)
    def monitor():
        while time.monotonic()-started<duration:
            metrics=sample()
            metrics['processes']=[r for r in metrics['processes'] if r.strip().split()[-1].split('/')[-1].lower() in {'java','ollama'}]
            samples.append(metrics)
            (ROOT/'muestras.json').write_text(json.dumps(samples,indent=2))
            time.sleep(15)
    thread=threading.Thread(target=monitor,daemon=True)
    thread.start()
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool: persisted=list(pool.map(worker,range(6)))
    summary={}
    for long in [False,True]:
        group=[r for r in rows if r['long']==long]
        completed=[r['seconds'] for r in group if r['state']=='done']
        summary['long' if long else 'short']=dict(status=Counter(r['state'] for r in group),
            median=statistics.median(completed) if completed else None,
            p95=statistics.quantiles(completed,n=20,method='inclusive')[18] if len(completed)>1 else None)
    result=dict(seconds=round(time.monotonic()-started,2),summary=summary,persisted=persisted,
        by_port={p:Counter(r['state'] for r in rows if r['port']==p) for p in [8090,8091]},samples=samples,rows=rows)
    (ROOT/'resistencia.json').write_text(json.dumps(result,indent=2))
    assert all(n*2==m and code==200 for _,n,m,code in persisted)
    assert all(r['state'] in {'done','truncated','MODEL_BUSY'} for r in rows)
    print(summary)
run()
