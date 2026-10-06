import {sql} from '../../../lib/db.mjs';
import {authorized,json,preflight} from '../../../lib/auth.mjs';
import {validateAnimal} from '../../../lib/validate.mjs';
import {sameAnimal,canUpdateAnimal} from '../../../lib/animal-sync.mjs';
export const runtime='nodejs';
export function OPTIONS(req){return preflight(req)}
export async function GET(req){if(!authorized(req))return json(req,{error:'Entre novamente em Conexão.'},401);try{const url=new URL(req.url);const after=Number(url.searchParams.get('after')||0);if(!Number.isSafeInteger(after)||after<0)return json(req,{error:'Cursor inválido'},400);const rows=await sql()`SELECT seq,id,chip,name,color,sex,block,kennel,photo,created,deleted,revision FROM animals WHERE seq>${after} ORDER BY seq LIMIT 2`;return json(req,{animals:rows.map(r=>({...r,seq:Number(r.seq)})),next:rows.length?Number(rows.at(-1).seq):after,more:rows.length===2})}catch(e){console.error('Leitura indisponível',e.message);return json(req,{error:'Não foi possível ler os registros online.'},503)}}
export async function POST(req){if(!authorized(req))return json(req,{error:'Entre novamente em Conexão.'},401);let r;try{if(Number(req.headers.get('content-length'))>1400000)return json(req,{error:'Foto muito grande'},413);const body=await req.text();if(body.length>1400000)return json(req,{error:'Foto muito grande'},413);r=validateAnimal(JSON.parse(body))}catch(e){return json(req,{error:e.message},400)}try{const db=sql();
// Inserts are idempotent. A revision prevents stale offline edits overwriting newer data.
if(r.serverRevision===0){const inserted=await db`INSERT INTO animals (id,chip,name,color,sex,block,kennel,photo,created,deleted,revision) VALUES (${r.id},${r.chip},${r.name},${r.color},${r.sex},${r.block},${r.kennel},${r.photo},${r.created},${r.deleted},1) ON CONFLICT DO NOTHING RETURNING id,revision`;if(inserted.length)return json(req,{id:r.id,status:'synced',revision:inserted[0].revision});}
const rows=await db`SELECT id,chip,name,color,sex,block,kennel,photo,created,deleted,revision FROM animals WHERE chip=${r.chip} OR id=${r.id}`;const saved=rows.find(x=>x.id===r.id);
if(saved&&sameAnimal(saved,r))return json(req,{id:r.id,status:'synced',revision:saved.revision});
if(saved&&canUpdateAnimal(saved,r)){
 const updated=await db`UPDATE animals SET name=${r.name},color=${r.color},sex=${r.sex},block=${r.block},kennel=${r.kennel},photo=${r.photo},deleted=${r.deleted},revision=revision+1,seq=nextval(pg_get_serial_sequence('animals','seq')) WHERE id=${r.id} AND chip=${r.chip} AND revision=${r.serverRevision} RETURNING id,revision`;
 if(updated.length)return json(req,{id:r.id,status:'synced',revision:updated[0].revision});
}
return json(req,{error:'Este cadastro foi alterado em outro aparelho ou o chip já existe online. A alteração local foi preservada para conferência.'},409);
}catch(e){console.error('Gravação indisponível',e.message);return json(req,{error:'Não foi possível salvar online. A alteração continua no aparelho.'},503)}}
