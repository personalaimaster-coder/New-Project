# Smoke test checklist

These steps validate the four highest-risk behaviours before any release:
reminder fires when the app is killed, reboot recovery, timezone change
reschedules, and mark-as-taken round-trips through the timeline.

## Pre-flight

1. Install a debug build on a physical device (emulator works for most steps,
   but battery-optimisation and OEM behaviour can only be verified on a
   real device).
2. Open the app and create a pet on the first-run screen.
3. Tap **Add medication**:
   - Name: `Test Med`
   - Dosage: `1 tablet`
   - Frequency: **Specific times daily**
   - Add a time **2 minutes from now** (use 24-hour format).
   - Save.
4. When prompted, grant the three permissions on the timeline card:
   - Notifications (API 33+)
   - Schedule exact alarms
   - Ignore battery optimization

## Test 1 — Alarm fires when app is killed (P0)

1. Force-stop the app: `adb shell am force-stop com.example.petmeds.debug`
2. Wait until the scheduled time.
3. **Pass criteria:** notification appears with title `Time for <Pet>'s medication`,
   medication name and dose visible, two actions (`Mark taken`, `Skip`).
4. Tap **Mark taken**. Re-open the app.
5. **Pass criteria:** the dose row in the timeline shows status `Taken`.

## Test 2 — Reboot recovery within 30s of unlock (P0)

1. Add a medication scheduled 5 minutes from now.
2. Reboot the device: `adb reboot`.
3. Unlock the device immediately when boot completes.
4. **Pass criteria:** within 30 seconds the alarm has been re-scheduled (you can
   verify with `adb shell dumpsys alarm | grep petmeds`). When the scheduled
   time arrives, the notification fires.

## Test 3 — Timezone change reschedules (P1)

1. Add a medication scheduled at `09:00` daily, several hours from now.
2. Change the device timezone (`Settings → System → Date & time → Time zone`).
3. **Pass criteria:** alarms re-scheduled. `adb shell dumpsys alarm | grep petmeds`
   shows the next dose at the new local 09:00.

## Test 4 — Mark taken / skipped round-trip (P0)

1. From the timeline, tap **Mark as taken** on a pending row.
2. **Pass criteria:** row immediately updates to `Taken` status; action buttons
   disappear.
3. Repeat with **Skip dose**; row should show `Skipped`.

## Test 5 — Edit medication cancels and reschedules (P1)

1. Tap **Edit medication** on a row.
2. Change the dose times to a single new time 1 minute from now and save.
3. **Pass criteria:** old alarms cancelled. Verify with
   `adb shell dumpsys alarm | grep petmeds` — old slots gone, new slot present.
4. The new alarm fires at the new time.

## Test 6 — Soft-delete preserves logs (P2)

1. Mark several doses as taken.
2. Delete the medication (UI deferred; via DAO in tests). Reopen timeline.
3. **Pass criteria:** historical `TAKEN` rows still readable in the database
   (verify via `adb shell run-as com.example.petmeds.debug ls databases/`),
   medication is not listed in active meds.

## Test 7 — Notification action buttons (P1)

1. When a notification is showing, tap **Mark taken** directly on the notification.
2. **Pass criteria:** notification dismisses, dose log updates without opening
   the app.

## Test 8 — Lock-screen privacy (P2)

1. While device is locked, trigger a reminder.
2. **Pass criteria:** lock-screen shows a generic "Medication reminder" — pet
   name and medication name are hidden until unlock.

## OEM-specific battery-optimisation regression (P0 on Xiaomi/Oppo/Vivo)

On Xiaomi/Oppo/Vivo/Realme devices, even after granting `IGNORE_BATTERY_OPTIMIZATIONS`
the OEM "App Lock" / "Auto-start" settings can still kill alarms.

1. Force-stop the app. Wait for a scheduled alarm.
2. If alarm does **not** fire, the OEM has a separate auto-start manager.
   Document the steps needed and surface them in the in-app permissions card
   in v1.1.

## Quick adb helpers

```
# View pending alarms
adb shell dumpsys alarm | grep -A2 petmeds

# Force-stop
adb shell am force-stop com.example.petmeds.debug

# Trigger a test broadcast (developer-only debug builds)
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED \
  -n com.example.petmeds.debug/com.example.petmeds.notifications.BootReceiver
```
