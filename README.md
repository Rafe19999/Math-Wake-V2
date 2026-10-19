# MathWake V1.3

MathWake is an Android alarm app that can be dismissed by either:
- solving 3 math questions, or
- walking 40 verified steps.

## What's new in V1.3
- Improved walking reliability using BOTH Step Detector (instant updates) and Step Counter (delayed catch-up)
- Reconciliation logic to reduce missed steps and delayed jumps
- Basic noise filtering for impossibly fast repeated step events
- Improved dark UI
- Added app logo and launcher icon
- Included GitHub Actions workflow for cloud APK builds

## Cloud build
Upload the full project, including the `.github` folder, to a GitHub repository and use the Actions tab to run the APK build workflow.
