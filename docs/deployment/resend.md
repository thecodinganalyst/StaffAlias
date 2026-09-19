# Resend configuration

StaffAlias uses Resend only for transactional account-activation email. **Resend is optional.** The backend starts and Platform Admin can create tenants when Resend is not configured. In that mode the activation email is skipped and a warning is logged; no API key or activation token is logged.

## 1. Configure Resend

1. In Resend, add and verify a StaffAlias sending domain (a dedicated subdomain such as `mail.example.com` is recommended).
2. Add the DNS records shown by Resend and wait for verification.
3. Create a sending-only API key, ideally restricted to the StaffAlias sending domain.
4. Choose the From address, for example `StaffAlias <no-reply@mail.example.com>`.

## 2. Create the GCP secret container

Terraform manages the Secret Manager resource named `staffalias-resend-api-key`, but deliberately does **not** manage the secret value.

Run the Terraform workflow/apply after this change. Then add a secret version:

```sh
printf '%s' 're_xxx' | gcloud secrets versions add staffalias-resend-api-key --data-file=-
```

Do not put the API key in Terraform variables, GitHub variables, source control, or documentation.

## 3. Configure deployment variables

Set these GitHub **production environment variables**:

- `RESEND_FROM` — verified sender, e.g. `StaffAlias <no-reply@mail.example.com>`
- `STAFFALIAS_FRONTEND_URL` — public frontend origin used to build activation links, e.g. `https://staffalias.web.app`

The backend deployment workflow checks whether `staffalias-resend-api-key` has an enabled version. If it does, it binds the secret as `RESEND_API_KEY`. If it does not, deployment continues without the binding.

## Local development

Resend is not required. Leave `RESEND_API_KEY` and `RESEND_FROM` unset. To test real delivery:

```sh
export RESEND_API_KEY=re_xxx
export RESEND_FROM='StaffAlias <no-reply@your-verified-domain>'
export STAFFALIAS_FRONTEND_URL=http://localhost:5173
```

## Runtime behaviour

| Configuration | Behaviour |
| --- | --- |
| API key + From address configured | Activation email is submitted to Resend |
| API key missing | Tenant creation succeeds; email is skipped with a warning |
| From address missing | Tenant creation succeeds; email is skipped with a warning |
| Resend API/network error | Tenant creation succeeds; delivery failure is logged |

The tenant administrator remains disabled until the one-time activation link is used. Activation links expire after 24 hours and the database stores only a SHA-256 hash of the token.
