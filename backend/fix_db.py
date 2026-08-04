import sqlite3
conn = sqlite3.connect('internship.db')
conn.execute("UPDATE ai_config SET chat_model='gemini-flash-latest'")
conn.commit()
cur = conn.cursor()
cur.execute("SELECT id, embedding_model, chat_model FROM ai_config")
print(cur.fetchall())
conn.close()
print("Done!")
