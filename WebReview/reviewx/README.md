# ReviewX

ReviewX is a modern review-commerce website focused on helping users decide what is worth buying.

## Setup

1. Install dependencies:

```bash
npm install
```

2. Create local env from sample:

```bash
cp .env.example .env.local
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env.local
```

3. Set `REVIEWX_ADMIN_TOKEN` in `.env.local`.

4. Run dev server:

```bash
npm run dev
```

## Build and checks

```bash
npm run build
npm run lint
```

## Security baseline

- `/admin/*` routes are protected by `proxy.ts` via `reviewx_admin_session` cookie and `REVIEWX_ADMIN_TOKEN` match.
- Keep secrets only in server environment variables.
- Do not expose Shopee/private cookies to client UI.

## Seed script

```bash
npm run prisma:seed
```

Note: full Prisma migrations and real DB integration are still pending checklist items in project TODO.
