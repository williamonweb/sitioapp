## Ícone do aplicativo (versão 2.5)

Ícone de patinha branca com fundo verde, adaptável ao formato do launcher Android,
incluindo ícone monocromático para temas compatíveis. Gere um novo APK para vê-lo.
Mantém o cadastro de bloco/canil, câmera direta e relatórios profissionais.

## Bloco e canil separados (versão 2.4)

O cadastro pergunta o bloco (ex.: A) e depois o canil (ex.: 2). Ambos são
obrigatórios nos novos registros. Registros antigos permanecem preservados,
com canil Não informado. CSV, Excel, backups e sincronização incluem o canil.
Relatórios filtram bloco e canil, e o resumo agrupa cada combinação.
Se já usa Neon, execute ATUALIZAR_CANIL_NEON.sql antes de sincronizar.
Copie este projeto sobre o atual e gere um novo APK pelo GitHub Actions.

## Excel profissional (versão 2.3)

Em Relatórios, toque em Baixar Excel. A planilha usa os registros já cadastrados
com os filtros escolhidos, título, data de emissão, cabeçalhos fixos, filtros,
linhas alternadas, impressão em A4 e resumo por bloco com totais por sexo.
As fotos são referenciadas por nome de arquivo; os backups mantêm as imagens.
Inclui a abertura direta da câmera da versão 2.2.

## Câmera direta no Android (versão 2.2)

No APK, o botão **Tirar foto do animal** abre diretamente o aplicativo de câmera.
Tire a foto e confirme para vê-la no cadastro. Cancelar mantém a foto anterior.
No site pelo navegador, a abertura depende do suporte do navegador ao capture.

## Atualização 2.1: macho ou fêmea

Copie os arquivos deste ZIP sobre a pasta do projeto existente, sem apagar `.git`
ou `.env.local`. Rode git add -A, git commit e git push. Gere um NOVO APK em
Actions > Gerar APK Android > Run workflow e baixe o artefato dessa execução.

Se já usa Neon, execute ATUALIZAR_SEXO_NEON.sql no SQL Editor ANTES de sincronizar.
A atualização preserva os registros antigos, com sexo Não informado. Para um
banco novo, BANCO_NEON.sql já inclui a alteração.

Faça backup com fotos antes de atualizar o aplicativo. Tente instalar o APK por
cima do existente. APKs debug gerados em execuções diferentes podem ter assinaturas
diferentes. Se o Android recusar a atualização, guarde/exporte o backup primeiro;
só depois reinstale e importe o backup. Não desinstale sem uma cópia dos pendentes.

# Chips Sítio — cadastro, app Android e relatórios

Cadastro por perguntas: chip obrigatório, nome opcional, cor, sexo, bloco, foto
opcional e revisão. Registros únicos por chip, funcionamento offline,
sincronização protegida por senha e relatório Excel com filtro por bloco/data.

## O que está entregue

- Site para Vercel, com API Next.js e PostgreSQL Neon.
- App Android nativo com interface local, arquivos internos e câmera.
- Página de relatórios em `/relatorios`, também acessível no app.
- Excel `.xlsx`: aba Animais e aba Resumo por bloco. Chip como texto,
  datas como datas, filtros e cabeçalho fixo. Inclui o nome do arquivo da foto;
  as imagens completas ficam no app, no banco e no backup JSON.
- Exportação/importação de backup com fotos; aceita o JSON da primeira versão.
- Código do Android e workflow para gerar APK. Este ZIP NÃO contém APK compilado.

## 1. Configurar o Neon

Crie um banco/projeto exclusivo para este sistema e copie a conexão.
No SQL Editor do Neon, execute `BANCO_NEON.sql` inteiro uma vez.
Alternativa local: crie `.env.local`, configure DATABASE_URL e execute:

```bash
npm install
node --env-file=.env.local scripts/setup-db.mjs
```

A tabela animals guarda o chip como TEXT para manter os zeros. A foto é
JPEG comprimido, armazenado como texto junto ao registro. Para o cadastro
do sítio isso simplifica a cópia/restauração; para um acervo grande, migre
as fotos para armazenamento de arquivos, preservando os registros.

## 2. Subir no GitHub e publicar na Vercel

Extraia o projeto numa pasta NOVA. Não sobrescreva outro sistema.
Abra um terminal nessa pasta:

```bash
npm install
npm test
npm run build
git init
git add .
git commit -m "Cria cadastro de chips com app offline e relatórios"
git branch -M main
git remote add origin URL_DO_SEU_REPOSITORIO
git push -u origin main
```

Importe esse repositório na Vercel como Next.js. Adicione estas variáveis
em Settings > Environment Variables, antes de publicar:

| Variável | Valor |
| --- | --- |
| DATABASE_URL | Conexão do banco Neon, com sslmode=require |
| APP_PASSWORD | Sua senha de acesso, no mínimo 12 caracteres |
| SESSION_SECRET | Segredo aleatório de pelo menos 32 caracteres |
| APP_ALLOWED_ORIGIN | https://appassets.androidplatform.net |

Gere SESSION_SECRET com:

```bash
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
```

Nunca coloque a conexão do banco ou os segredos dentro dos arquivos públicos.
Copie `.env.example` para `.env.local` para testar localmente. Para usar o dev:

```bash
npm run dev
```

## 3. Usar o site

Abra o link publicado. Vá a Conexão, informe a senha e clique em Conectar
e sincronizar. No site, o endereço já fica preenchido.
O cadastro é salvo primeiro no aparelho; só recebe a marca Sincronizado
quando o servidor confirma. Ao salvar, recuperar internet, abrir o sistema
e a cada minuto com o sistema aberto, há tentativa de sincronização.
No navegador, abra o site online pelo menos uma vez para carregar os arquivos
offline. O site instalado na tela inicial continua dependendo do armazenamento
do navegador; o app Android é a opção com arquivos próprios.

Relatórios: abra `https://SEU-PROJETO.vercel.app/relatorios`. Conecte e
sincronize para trazer os registros salvos por outros aparelhos. Filtre por
bloco e datas e clique em Baixar Excel. O relatório inclui também os pendentes
deste aparelho e mostra o status; registros de outros aparelhos só aparecem
depois de sincronizados.

## 4. Gerar o APK Android pelo GitHub

O ZIP inclui `.github/workflows/android-apk.yml` (pasta oculta).
Depois de subir TODOS os arquivos no GitHub:

1. Abra o repositório > Actions.
2. Escolha "Gerar APK Android" > Run workflow.
3. Quando terminar, abra a execução e baixe o artefato "Chips-Sitio-APK".
4. Extraia o artefato e transfira `app-debug.apk` para seu Android.
5. Instale, autorizando a instalação deste arquivo quando o Android pedir.
6. Abra Chips Sítio > Conexão, informe o link Vercel e a senha.

Esse é um APK de teste assinado automaticamente. Para distribuição permanente,
gere uma versão release assinada e GUARDE a chave de assinatura. Uma mudança
de assinatura pode exigir desinstalar o app; sincronize e exporte antes disso.
O workflow exige internet e execução de GitHub Actions habilitada na conta.
Ele não foi executado a partir deste ambiente.

Alternativa: abra a pasta `android` no Android Studio, sincronize o Gradle,
instale SDK 35 e use Build > Build APK(s). Projeto usa Java 17/Gradle 8.9.
Ao modificar a interface web, antes de compilar Android execute:

```bash
npm run android:assets
```

## 5. Conferência no sítio

Antes de ir: instale o app e teste cadastro, foto, fechamento/reabertura e
sincronização. Abra o link em outro aparelho e confirme a recuperação da foto.
No sítio, cadastre normalmente sem internet. Faça um backup após cada bloco.
No Android, salvar Excel/backup abre o compartilhamento: escolha Drive,
Arquivos ou outro aplicativo para manter uma cópia fora do app.

Não desinstale nem limpe os dados do app antes de sincronizar os pendentes.
Arquivos internos resistem à limpeza do navegador, mas não à desinstalação,
perda/dano do celular ou limpeza dos dados do app.

## 6. Recuperar registros

Novo aparelho: instale/abra, conecte ao MESMO link e sincronize. Registros
online e fotos são baixados. Ou importe o backup JSON em Registros.
Backups importados são considerados pendentes até confirmação do servidor.
Não troque o endereço do servidor durante uma sincronização.

Se houver outro registro com o mesmo chip online, o sistema preserva o local
e mostra o conflito. Confira o chip antes de continuar; não há sobrescrita
silenciosa nem edição de registros já salvos nesta versão.

## Validação e limites

Testes automatizados cobrem: senha/token, campos, zeros iniciais, sincronização
sem internet, confirmação do servidor, duplicidade, repetição segura após falha
e recuperação de fotos em novo aparelho. Exportação Excel foi lida por uma
ferramenta independente, com verificação de tipos, datas, filtros e totais.
Build de produção Next.js verificado. Conexão real Neon/Vercel e APK dependem
da configuração/publicação e de compilação Android; não estão ativos no ZIP.

A sincronização é feita com a interface aberta. Não há serviço Android em
segundo plano. Existe uma senha compartilhada para este sistema, não contas
separadas por funcionário. Sessão dura 30 dias. Login tem limite persistente
de 10 tentativas por IP em 15 minutos. Dados locais não têm bloqueio por senha.

## Referências de implementação

- Next.js: https://nextjs.org/docs
- Neon serverless driver: https://neon.com/docs/serverless/serverless-driver
- Android WebViewAssetLoader: https://developer.android.com/develop/ui/views/layout/webapps/load-local-content
