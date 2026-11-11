# Deploy on Render (Docker) with an Aiven / external MySQL

Render builds the `Dockerfile` in the repo root, so `pom.xml`, `Dockerfile` and `src/` must sit
directly in the root of the GitHub repository (not inside an extra sub-folder).
Check from the project folder:  `git ls-files | head -5`  ->  Dockerfile, pom.xml, src/...

## Environment variables (Render -> your service -> Environment)

| Key | Value |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://HOST:PORT/DBNAME?sslMode=REQUIRED` (from Aiven) |
| `SPRING_DATASOURCE_USERNAME` | database user (Aiven: usually `avnadmin`) |
| `SPRING_DATASOURCE_PASSWORD` | database password |
| `BREVO_API_KEY` | Brevo API key - see EMAIL_SETUP.md (needed for mails on Render free) |
| `MAIL_FROM` | sender email verified in Brevo |
| `OWNER_EMAIL` | address that receives "new order" mails |
| `OWNER_UPI_ID` | your UPI id |
| `OWNER_ACCESS_CODE` | secret code for the owner orders page (page stays locked while empty) |

Optional: `MAIL_FROM_NAME` (default `FoodCourt`), `MAIL_USERNAME` / `MAIL_PASSWORD`
(only for Gmail SMTP on your own PC or a paid host).

Never put passwords in files that are pushed to GitHub - GitHub blocks pushes that contain secrets.

## Deploy / redeploy
1. `git add -A`  ->  `git commit -m "update"`  ->  `git push`
2. Render redeploys automatically (or press *Manual Deploy -> Deploy latest commit*).
3. In the logs look for:
   * `HikariPool-1 - Start completed`        (database connected)
   * `MAIL: using Brevo HTTPS API`            (mail configured)
   * `Started ThymeleafCurrypointApplication`

## Running on your own PC
The datasource has no built-in default any more, so set the three `SPRING_DATASOURCE_*`
variables (for a local MySQL: `jdbc:mysql://localhost:3306/foodcourt`, user `root`, your password)
in your IDE run configuration or terminal before starting the app.
