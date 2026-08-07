# pyrefly: ignore [missing-import]
from fastapi import FastAPI
# pyrefly: ignore [missing-import]
from fastapi.middleware.cors import CORSMiddleware
# pyrefly: ignore [missing-import]
from fastapi.staticfiles import StaticFiles
# pyrefly: ignore [missing-import]
from fastapi.responses import FileResponse
import models
from database import engine, SessionLocal
import auth
import os

# Create tables (including new ones)
models.Base.metadata.create_all(bind=engine)

# ─── Positions seed data ──────────────────────────────────────────────────────
POSITIONS = [
    {"name": "Trợ lý dự án",  "is_manager": False},
    {"name": "PM",             "is_manager": True},
    {"name": "DU Lead",        "is_manager": True},
    {"name": "GDTT",           "is_manager": True},
    {"name": "PGDTT",          "is_manager": True},
    {"name": "Dev",            "is_manager": False},
    {"name": "Dev Lead",       "is_manager": True},
    {"name": "Dev Mobile",     "is_manager": False},
    {"name": "DevOps",         "is_manager": False},
    {"name": "Tester",         "is_manager": False},
    {"name": "Test Lead",      "is_manager": True},
    {"name": "BA",             "is_manager": False},
    {"name": "BA Lead",        "is_manager": True},
    {"name": "QA",             "is_manager": False},
    {"name": "DA",             "is_manager": False},
    {"name": "AI",             "is_manager": False},
]


def seed_positions():
    db = SessionLocal()
    try:
        for p in POSITIONS:
            existing = db.query(models.Position).filter(models.Position.name == p["name"]).first()
            if not existing:
                db.add(models.Position(**p))
        db.commit()
        print("[OK] Positions seeded")
    finally:
        db.close()


def seed_admin():
    db = SessionLocal()
    try:
        # ── Migration: rename role 'intern' → 'user' ──
        old_interns = db.query(models.User).filter(models.User.role == "intern").all()
        for u in old_interns:
            u.role = "user"
            if not u.user_type:
                u.user_type = "intern"
        if old_interns:
            db.commit()
            print(f"[OK] Migrated {len(old_interns)} intern role(s) → user")

        # ── Ensure admin account exists ──
        existing = db.query(models.User).filter(models.User.employee_code == "admin").first()
        if not existing:
            admin_user = models.User(
                employee_code="admin",
                full_name="Quản trị viên",
                role="admin",
                user_type="admin",
                account_status=1,
            )
            db.add(admin_user)
            db.flush()

            admin_acc = models.Account(
                user_id=admin_user.id,
                username="admin",
                password=auth.hash_password("Admin@123")
            )
            db.add(admin_acc)
            db.commit()
            print("[OK] Admin account created: admin / Admin@123")
    finally:
        db.close()


seed_positions()
seed_admin()

app = FastAPI(title="Intern & Employee Management API", version="2.0.0")

# pyrefly: ignore [missing-import]
from fastapi.exceptions import RequestValidationError
# pyrefly: ignore [missing-import]
from fastapi.responses import JSONResponse
# pyrefly: ignore [missing-import]
from fastapi import Request

@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    body = await request.body()
    print("========================================")
    print("VALIDATION ERROR: ", exc.errors())
    print("REQUEST BODY: ", body)
    print("HEADERS: ", request.headers)
    print("========================================")
    return JSONResponse(status_code=422, content={"detail": exc.errors()})

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Routers
from routers import auth_router, user_router, admin_router, schedule_router
from routers import employee_router, documents_router, ai_config_router, chat_router, overtime_router
app.include_router(auth_router.router)
app.include_router(user_router.router)
app.include_router(admin_router.router)
app.include_router(schedule_router.router)
app.include_router(employee_router.router)
app.include_router(documents_router.router)
app.include_router(ai_config_router.router)
app.include_router(chat_router.router)
app.include_router(overtime_router.router)

# Serve frontend (Cấu hình no-cache tuyệt đối để trình duyệt luôn nạp mã mới)
frontend_dir = os.path.join(os.path.dirname(__file__), "..", "frontend")
if os.path.exists(frontend_dir):
    app.mount("/static", StaticFiles(directory=os.path.join(frontend_dir, "static")), name="static")

    NO_CACHE_HEADERS = {
        "Cache-Control": "no-cache, no-store, must-revalidate",
        "Pragma": "no-cache",
        "Expires": "0"
    }

    @app.get("/")
    def serve_index():
        return FileResponse(os.path.join(frontend_dir, "index.html"), headers=NO_CACHE_HEADERS)

    @app.get("/{path:path}")
    def serve_spa(path: str):
        fp = os.path.join(frontend_dir, path)
        if os.path.exists(fp) and not os.path.isdir(fp):
            return FileResponse(fp, headers=NO_CACHE_HEADERS)
        return FileResponse(os.path.join(frontend_dir, "index.html"), headers=NO_CACHE_HEADERS)

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
