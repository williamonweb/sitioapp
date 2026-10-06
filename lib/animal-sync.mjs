// Shared decisions for retries and optimistic updates.
export function sameAnimal(saved,r){return saved.id===r.id&&Boolean(saved.deleted)===r.deleted&&['chip','name','color','block','photo','sex','kennel'].every(k=>(saved[k]||'')===(r[k]||''))}
export function canUpdateAnimal(saved,r){return saved.id===r.id&&saved.chip===r.chip&&saved.revision===r.serverRevision}
