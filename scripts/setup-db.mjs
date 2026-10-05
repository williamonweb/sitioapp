import {sql} from '../lib/db.mjs';
const db=sql();
await db`CREATE TABLE IF NOT EXISTS animals (seq BIGSERIAL PRIMARY KEY,id UUID UNIQUE NOT NULL,chip TEXT UNIQUE NOT NULL,name TEXT NOT NULL DEFAULT '',color TEXT NOT NULL,block TEXT NOT NULL,photo TEXT NOT NULL DEFAULT '',created TIMESTAMPTZ NOT NULL)`;
await db`CREATE TABLE IF NOT EXISTS login_limits (key TEXT NOT NULL,bucket BIGINT NOT NULL,attempts INTEGER NOT NULL,PRIMARY KEY(key,bucket))`;
console.log('Banco configurado.');
