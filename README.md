# Free AI Chat — Android

A lightweight Android AI chat app using Google's Gemini Developer API.

## What it includes

- Chat interface with multi-turn conversation history
- Gemini 3.6 Flash model
- API-key setup screen
- Local history stored on the phone
- No paid SDK or server required
- GitHub Actions workflow that builds a debug APK

## Important

The Gemini API has a free developer tier, but it has rate/usage limits. An API key is required. The app sends the key directly to Google's API over HTTPS; for a personal app this is convenient, but do not publish your own API key inside the APK.

## Build with only an Android phone

1. Create a **public** GitHub repository from your phone.
2. Upload this whole project folder (GitHub's web upload supports folders/files; alternatively upload the ZIP contents).
3. Open the **Actions** tab and run **Build Android APK**.
4. When it finishes, open the workflow run → **Artifacts** → download `FreeAIChat-debug-apk.zip`.
5. Extract the ZIP on your phone and install the APK. Android may ask you to allow installs from your browser/file manager.

The workflow uses a standard GitHub-hosted runner. Public repositories can use standard GitHub Actions runners without a paid Actions bill according to GitHub's current documentation.
