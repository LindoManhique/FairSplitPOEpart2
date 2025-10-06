 FairSplit (Part 2 Prototype)

A minimal Android app for splitting expenses in groups. Built for the OPSC6312 POE — **Part 2: App Prototype Development**.

Repo Link : https://github.com/LindoManhique/FairSplitPOEpart2.git


YouTube Link  : https://youtu.be/GEnWz75iz-A


---

## What this prototype includes

- ✅ Email **Register / Login** (Firebase Auth)
- ✅ **Settings**: change Display Name & Base Currency (saved to Firestore)
- ✅ **REST API** call (ExchangeRate-API) via **Retrofit** to fetch currency rate
- ✅ **Groups**: create & list
- ✅ **Expenses**: add & list per group
- ✅ **Bottom navigation** (Groups / Expenses / Settings)
- ✅ Simple **unit test** (`SplitCalculatorTest`)

> POE-only items (SSO, FCM push, multilanguage, full offline sync) are **out of scope** for Part 2.

---

## Tech Stack

- Kotlin, AndroidX, Material Components  
- Firebase **Auth** & **Firestore** (via Firebase SDK)
- **Retrofit** + Gson + OkHttp logging
- JUnit4 for unit tests
- Compile SDK 34, Min SDK 24

---

## Prerequisites

- **Android Studio** (Hedgehog/Koala or newer)
- **Android SDK 34** installed
- An emulator or a physical Android 8.0+ device

> The project includes `google-services.json`. If you’re using your **own** Firebase project, replace it with your file (Project settings → Android app → download `google-services.json`).

---

## Quick Start (5 steps)

1. **Clone**
   ```bash
   git clone <your-repo-url> fairsplit
   cd fairsplit
Open in Android Studio
File → Open… → select the project root → Trust if prompted.

Sync Gradle
Android Studio will auto-sync. If not: File → Sync Project with Gradle Files.

Run

Choose an emulator (Pixel, API 34) or a plugged-in device (USB debugging on).

Click Run ▶.

Create an account

On first open, tap Create account and register with any email/password (min 6 chars).

You’ll be able to login immediately.

How to Use (demo path)
Login with your email + password.

Settings

Change Display name and Currency (3-letter code, e.g., ZAR) → Save.

Optional: tap Fetch ZAR → USD to hit the REST API and display the rate.

Groups

Enter a group name (e.g., Family) → Create group.

Tap the new group to open it.

Expenses

Add an expense (title + amount) → Add expense (appears in the list).

Navigation

Use the bottom bar to switch between Groups, Expenses (last opened group), and Settings.

Troubleshooting

Infinite spinner / “timed out”

Check network; try again. We use safe timeouts & show a toast if the network is slow.

Login succeeds but UI doesn’t update

Use the back button to return; controllers refresh data in onStart().

Password reset

Reset email uses Firebase. On emulators without Google services, it may silently fail.

Build fails after renaming packages

Ensure AndroidManifest.xml activity names match the package of your view/* classes.

Gradle sync issues

File → Invalidate Caches / Restart…, then re-sync.

Credits / AI Use (≤500 words guideline)

I designed and implemented the FairSplit prototype myself. This includes planning the screen flows, defining the data models (UserProfile, Group, Expense), implementing controllers for auth/settings/groups/expenses, integrating Firebase Auth/Firestore, wiring Retrofit to an external rates API, adding validation and error handling, and building the bottom navigation and unit test.
When I hit roadblocks, I used an AI assistant **as a debugging partner**, not a code generator. Typical uses:
- **Error triage & fixes:** Interpreting Gradle/manifest errors, Android 12 `android:exported` changes, and view-binding mismatches.
- **Kotlin/Coroutines guidance:** Avoiding spinner lockups, proper use of `withContext`, timeouts, and `tasks.await()`.
- **Retrofit patterns:** Confirming interface signatures, Gson converter setup, and OkHttp logging usage.
- **Firebase best practices:** Auth + Firestore usage patterns, simple repository structure, and null-safety checks.
- **XML/UI polish:** Addressing accessibility warnings (touch target sizes), adding titles, and simple Material components.
- **Small snippets only:** Occasional boilerplate (e.g., adapters, menu XML, vector placeholders) that I reviewed and edited.

What I did **myself**:

- Requirements mapping from Part 1 → Part 2 outcomes; deciding which features to prototype now.
- Creating Activities and controllers; writing and adapting all business logic.
- Designing DTOs and Firestore collection layout; writing repository calls.
- Input validation, toasts, and defensive UI (no crashes on bad input).
- Bottom nav integration across screens; remembering last-opened group.
- The unit test and README; recording the demo flow.

Quality and ownership:

- **Every AI suggestion was reviewed, adapted, and tested by me** in Android Studio (emulator + device where possible).
- No credentials or private data were generated or stored by AI. The public rates endpoint was chosen and configured by me.

