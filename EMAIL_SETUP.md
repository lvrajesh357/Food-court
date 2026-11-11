# Getting emails to work on Render (free plan)

**Why mails never arrived:** Render's free plan blocks outbound SMTP ports (25, 465, 587),
so Gmail SMTP can never connect from there. The app now sends mail over **HTTPS** (port 443,
never blocked) through Brevo's free API (300 mails/day). It is also faster than SMTP.

## One-time setup (about 5 minutes)

1. Create a free account at **https://www.brevo.com** and finish the account activation it asks for.
2. **Add a sender:** Brevo -> *Senders, Domains & Dedicated IPs* -> *Senders* -> *Add a sender*.
   Use the email address that should appear as "From" (your Gmail is fine) and confirm the
   verification mail Brevo sends to it.
3. **Create an API key:** Brevo -> *SMTP & API* -> *API Keys* -> *Generate a new API key*. Copy it.
4. In Render open your service -> **Environment** and add:

| Key | Value |
|---|---|
| `BREVO_API_KEY` | the API key from step 3 |
| `MAIL_FROM` | the sender email you verified in step 2 |
| `MAIL_FROM_NAME` | (optional) shown as the sender name, default `FoodCourt` |
| `OWNER_EMAIL` | the address that receives "New order" mails |
| `OWNER_UPI_ID` | your UPI id |
| `OWNER_ACCESS_CODE` | secret code for `/owner/orders` (**required** - owner page stays locked while empty) |

   (Keep your existing `SPRING_DATASOURCE_*` variables as they are.)
5. Click **Manual Deploy -> Deploy latest commit**.

## How to check it works

Open the Render **Logs**. Right after start you should see:

    MAIL: using Brevo HTTPS API (sender: you@gmail.com). Works on Render free plan.

After placing an order or requesting an OTP:

    Email sent to ra***@gmail.com via Brevo API in 350 ms

If you see `Brevo refused the request ... sender is not verified`, finish step 2.
If you see `Brevo says the API key is invalid`, re-copy the key (step 3).
If you see `MAIL: using SMTP` plus a Render warning, `BREVO_API_KEY` is not set.

## Good to know

* Locally (your own PC) you can leave `BREVO_API_KEY` empty: the app falls back to Gmail SMTP
  using `MAIL_USERNAME` / `MAIL_PASSWORD` (a Gmail *app password*).
* Mails go out on a background thread, so the customer never waits for them.
* Render's free plan "sleeps" after ~15 minutes without visitors; the first request after that
  takes 30-60 seconds to wake the app (this is a Render limit, not a bug). A free uptime monitor
  (e.g. UptimeRobot pinging `https://your-app.onrender.com/login` every 5 minutes) keeps it awake.
* Brevo mails may land in *Spam* at first. Ask customers to mark them "Not spam", or verify a
  domain you own in Brevo for best delivery.
