import sqlite3
from google import genai
from google.genai import types as genai_types
import time

conn = sqlite3.connect('internship.db')
cur = conn.cursor()
cur.execute("SELECT api_key FROM ai_config LIMIT 1")
api_key = cur.fetchone()[0]
conn.close()

client = genai.Client(api_key=api_key)

CANDIDATES = [
    "gemini-flash-latest",
    "gemini-flash-lite-latest",
    "gemini-2.0-flash-lite",
    "gemini-2.5-flash",
    "gemini-2.5-flash-lite",
    "gemini-2.5-pro",
    "gemini-3.1-flash-lite",
]

print("=== Testing each model ===")
for model in CANDIDATES:
    try:
        res = client.models.generate_content(
            model=model,
            contents="Chào.",
            config=genai_types.GenerateContentConfig(temperature=0.1)
        )
        print(f"  OK  [{model}] => {res.text.strip()[:50]}")
    except Exception as e:
        err = str(e)[:120]
        print(f"  ERR [{model}] => {err}")
    time.sleep(1)
