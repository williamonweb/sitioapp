import {sql} from '../lib/db.mjs';
const db=sql();
await db`CREATE TABLE IF NOT EXISTS animals (seq BIGSERIAL PRIMARY KEY,id UUID UNIQUE NOT NULL,chip TEXT UNIQUE NOT NULL,name TEXT NOT NULL DEFAULT '',color TEXT NOT NULL,block TEXT NOT NULL,photo TEXT NOT NULL DEFAULT '',created TIMESTAMPTZ NOT NULL)`;
await db`CREATE TABLE IF NOT EXISTS login_limits (key TEXT NOT NULL,bucket BIGINT NOT NULL,attempts INTEGER NOT NULL,PRIMARY KEY(key,bucket))`;
await db`ALTER TABLE animals ADD COLUMN IF NOT EXISTS sex TEXT NOT NULL DEFAULT ''`;
await db`ALTER TABLE animals ADD COLUMN IF NOT EXISTS kennel TEXT NOT NULL DEFAULT '';`;
console.log('Banco configurado.');
