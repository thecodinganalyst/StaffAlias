# Production frontend deployment

StaffAlias uses Firebase Hosting for the production React/Vite frontend and Google Cloud Run for the backend API.

## One-time Firebase setup

Use the same Google Cloud project that already hosts StaffAlias production resources.

1. Add Firebase to the existing Google Cloud project in the Firebase console.
2. Enable Firebase Hosting.
3. Create or identify the production Hosting site.
4. Record the Hosting site ID. The default Firebase domains will be:
   - `https://<site-id>.web.app`
   - `https://<site-id>.firebaseapp.com`

The GitHub deployment does not use a Firebase service-account JSON key. It reuses the GitHub OIDC / Workload Identity Federation configured for StaffAlias.

## Apply the Terraform update

After this change is merged, apply Terraform again so the deployment service account can deploy Firebase Hosting:

```bash
cd infrastructure/terraform
terraform init
terraform plan
terraform apply
```

This enables the Firebase management/Hosting APIs and grants `staffalias-deploy` the Firebase Hosting Admin and API Keys Viewer roles required by the Firebase CLI.

## GitHub production environment

Add one new non-secret Environment variable to the existing GitHub `production` Environment:

| Variable | Value |
| --- | --- |
| `FIREBASE_HOSTING_SITE` | Firebase Hosting site ID, for example `staffalias` |

The frontend workflow reuses these existing variables from issue #30:

- `GCP_PROJECT_ID`
- `GCP_REGION`
- `GCP_CLOUD_RUN_SERVICE`
- `GCP_WORKLOAD_IDENTITY_PROVIDER`
- `GCP_SERVICE_ACCOUNT`

No additional GitHub secret is required.

## Production API URL

Do not create or hard-code a production `VITE_API_URL` value.

The deployment workflow resolves the current Cloud Run service URL with `gcloud run services describe` and supplies that URL as `VITE_API_URL` when Vite builds the frontend. Local development continues to use `frontend/.env.local` and the localhost API URL.

## CORS

Production CORS is explicit rather than wildcard-based.

The backend deployment workflow derives the two standard Firebase Hosting origins from `FIREBASE_HOSTING_SITE` and injects them into Cloud Run as `CORS_ALLOWED_ORIGINS`:

```text
https://<site-id>.web.app
https://<site-id>.firebaseapp.com
```

The normal local backend configuration allows `http://localhost:5173` for Vite development.

After merging this change, run **Deploy Backend to Cloud Run** once before the first frontend deployment so the current Cloud Run revision receives the production CORS configuration.

If a custom frontend domain is added later, update the backend deployment configuration before serving the app from that domain.

## Deploy

The recommended first production sequence is:

1. Apply Terraform.
2. Add `FIREBASE_HOSTING_SITE` to the GitHub `production` Environment.
3. Run **Deploy Backend to Cloud Run** from `main` so CORS is updated.
4. Confirm the backend workflow is healthy.
5. Run **Deploy Frontend to Firebase Hosting** from `main`.

The frontend workflow:

1. authenticates to GCP with OIDC,
2. resolves the production Cloud Run URL,
3. installs frontend dependencies,
4. runs lint, type checking, and tests,
5. builds Vite with the production API URL,
6. verifies the configured Firebase Hosting site exists,
7. deploys the `frontend/dist` output using Firebase CLI,
8. verifies the `web.app` URL is reachable, and
9. checks that the backend accepts a CORS preflight from that Firebase origin.

## SPA routing and caching

`firebase.json` rewrites unknown routes to `/index.html`, so React routes continue to work when a URL is refreshed or opened directly.

Vite-generated `/assets/**` files receive long-lived immutable caching. HTML is configured with `no-cache` so users receive the current application shell after a deployment.

## Rollback

Firebase Hosting retains release history. If a frontend release must be reverted, use the Firebase Hosting release history in the Firebase console to roll back to the previous known-good release.

A backend rollback is independent of the frontend and is documented in `docs/deployment/backend.md`.

After rollback, verify both the frontend URL and the backend `/actuator/health` endpoint.

## Troubleshooting

### Firebase site not found

Verify that `FIREBASE_HOSTING_SITE` exactly matches the Hosting site ID shown in Firebase Console > Hosting.

### Permission denied during Firebase deployment

Re-run Terraform and confirm the GitHub deployment identity is `staffalias-deploy@<project-id>.iam.gserviceaccount.com`. Do not replace OIDC with a service-account key.

### Frontend deploy succeeds but API calls fail in the browser

Check the browser network console for CORS errors. Re-run the backend deployment after confirming `FIREBASE_HOSTING_SITE`, then re-run the frontend workflow.

### Frontend uses an unexpected API URL

Check the `Resolve production backend URL` step in the frontend workflow. The value is obtained from the configured Cloud Run service at deployment time and is not stored in the repository.
