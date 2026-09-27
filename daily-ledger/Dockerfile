FROM ghcr.io/cirruslabs/android-sdk:35

USER root
WORKDIR /workspace

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip python3 \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL https://services.gradle.org/distributions/gradle-8.9-bin.zip -o /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && rm /tmp/gradle.zip

ENV PATH="/opt/gradle-8.9/bin:${PATH}"

COPY . /workspace

RUN cd /workspace \
    && gradle --no-daemon :app:lintDebug :app:assembleDebug \
    && mkdir -p /site \
    && cp app/build/outputs/apk/debug/app-debug.apk /site/DailyLedger-v1.0.0.apk \
    && printf '%s\n' '<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>दैनिक हिशोब APK</title><style>body{font-family:system-ui;background:#000;color:#fff;display:grid;place-items:center;min-height:100vh;margin:0}main{max-width:520px;padding:32px;text-align:center}a{display:inline-block;padding:14px 22px;border:1px solid #555;border-radius:12px;color:#000;background:#fff;text-decoration:none;font-weight:700}</style></head><body><main><h1>दैनिक हिशोब</h1><p>Offline daily accounting APK</p><a href="/DailyLedger-v1.0.0.apk">Download APK</a></main></body></html>' > /site/index.html

EXPOSE 8080
CMD ["python3", "-m", "http.server", "8080", "--directory", "/site"]
