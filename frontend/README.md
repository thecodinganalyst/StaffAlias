# StaffAlias frontend

The StaffAlias frontend is an authenticated business-application shell built with:

- React 19 + TypeScript
- Vite
- Refine Core v5 (headless application/data framework)
- Ant Design v6
- React Router
- Vitest + React Testing Library
- ESLint

Refine Core is intentionally used headlessly. UI components come directly from Ant Design v6 so the application is not coupled to an older Refine/Ant Design adapter.

## Local development

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

The default local API URL is `http://localhost:8080`. Override it with:

```text
VITE_API_URL=https://your-api.example.com
```

Production deployments must supply the appropriate `VITE_API_URL` at build time. Do not hard-code environment-specific API endpoints in application source.

## Quality checks

```bash
npm run lint
npm run typecheck
npm run test
npm run build
```

## Structure

```text
src/
├── api/            # centralized HTTP client and Refine data provider
├── auth/           # authentication contracts; provider implementation comes later
├── components/     # shared application shell and error boundary
├── config/         # environment and Ant Design theme configuration
├── pages/          # route-level pages
├── tenant/         # tenant-state contracts; authenticated resolution comes later
└── test/           # shared test setup
```

## Tenant and authentication boundaries

The frontend does not hard-code a tenant and does not treat a client-selected tenant ID as authorization. The placeholder auth and tenant contracts provide extension points for future authenticated tenant resolution. The backend remains responsible for enforcing tenant isolation.
