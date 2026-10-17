# Deploying CurryPoint to Railway

Railway can host both the Spring Boot app and a managed MySQL database in one
project, which is why it's the better fit here (Render's free tier only offers
managed Postgres, not MySQL).

## 1. Push this project to GitHub
Railway deploys from a GitHub repo.
```
git init
git add .
git commit -m "Initial commit"
gh repo create currypoint --private --source=. --push
```
(or create the repo on github.com and `git push` manually)

## 2. Create the Railway project
1. Go to https://railway.app -> **New Project**.
2. **Deploy from GitHub repo** -> pick your `currypoint` repo.
   Railway detects the `Dockerfile` and builds/runs the app automatically.
3. In the same project, click **+ New** -> **Database** -> **Add MySQL**.
   Railway provisions a managed MySQL instance and exposes connection details
   as variables (`MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`,
   `MYSQLPASSWORD`) on the MySQL service.

## 3. Set environment variables on the app service
Open your app service -> **Variables** and add:

| Variable        | Value                                                                 |
|-----------------|------------------------------------------------------------------------|
| `DB_URL`        | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}` |
| `DB_USERNAME`   | `${{MySQL.MYSQLUSER}}`                                                |
| `DB_PASSWORD`   | `${{MySQL.MYSQLPASSWORD}}`                                            |
| `MAIL_USERNAME` | your Gmail address                                                    |
| `MAIL_PASSWORD` | a Gmail **app password** (not your normal password — see step 4)     |
| `OWNER_EMAIL`   | address that should receive "new order" notifications                |

Railway lets you reference another service's variables with the
`${{ServiceName.VAR}}` syntax shown above — pick your MySQL service's exact
name from the dropdown it offers when you start typing `${{`.

`PORT` is set automatically by Railway; `application.properties` already
reads it (`server.port=${PORT:8989}`), so you don't need to set it yourself.

## 4. Generate a Gmail app password
Regular Gmail passwords don't work with SMTP anymore.
1. Turn on 2-Step Verification on the Google account: https://myaccount.google.com/security
2. Go to https://myaccount.google.com/apppasswords
3. Create an app password for "Mail" and use that 16-character value as `MAIL_PASSWORD`.

## 5. Deploy
Railway redeploys automatically on every push once variables are set. Watch
the **Deployments** tab for build logs. First boot will run
`spring.jpa.hibernate.ddl-auto=update`, which creates the tables
(`register`, `menuitems`, `snacks`, `Special_items`, `cart`, `orders`) in the
managed MySQL database automatically — no manual schema setup needed.

## 6. Load menu data
The app has no admin UI to add menu items yet — `menuitems`, `snacks`, and
`Special_items` need rows before the veg/non-veg/snacks/biryani pages show
anything. Easiest path: open Railway's MySQL **Data** tab (or connect with any
MySQL client using the connection details from step 2) and insert rows
directly, e.g.:
```sql
INSERT INTO menuitems (name, category, price, quantity) VALUES
  ('Paneer Butter Masala', 'veg', 220, 1),
  ('Chicken Curry', 'Non-veg', 260, 1);

INSERT INTO Special_items (name, category, price, quantity) VALUES
  ('Veg Biryani', 'veg-biryani', 180, 1),
  ('Chicken Biryani', 'non-veg-biryani', 240, 1);

INSERT INTO snacks (name, category, price) VALUES
  ('Spring Roll', 'snacks', 120);
```

## Local development (unchanged)
Running locally still works with zero setup beyond a local MySQL instance —
`application.properties` falls back to `localhost:3306/foodcourt` /
`root` / `1234` when the env vars aren't set. `spring-boot-devtools` is
already in `pom.xml`, so the app auto-restarts on code changes (Spring's
equivalent of a "live server") when run from an IDE or `mvn spring-boot:run`.
