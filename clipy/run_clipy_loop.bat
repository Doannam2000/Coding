@echo off
set ROUND=1

:loop
echo ==============================
echo CLIPY AUTO ROUND %ROUND%
echo ==============================

claude -p --continue --dangerously-skip-permissions "Read CLIPY_TODO_UX_UI_FULL.md. Continue building Clipy exactly according to the TODO file. Work through unchecked items from top to bottom. Do not stop after one screen or one feature. On Windows, use gradlew.bat assembleDebug. Test each feature seriously before marking it done. If build fails, fix the root cause and rerun. If export/tool/API returns success, verify the real output exists, file size is greater than 0, result screen opens it, recent exports contains it, and share/open does not crash. Do not modify package name, signing config, keystore files, .env, google-services.json, or production credentials. Only say CLIPY_DONE when all checklist items are done, Cut/Compress/Merge/Extract Audio/Slideshow work end-to-end, export files are verified, UI has no clipping or overlap, and gradlew.bat assembleDebug passes."

set /a ROUND=%ROUND%+1

echo Claude finished one round. Restarting in 10 seconds...
timeout /t 10

goto loop