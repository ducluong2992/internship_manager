from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
import models
from database import engine, SessionLocal
import auth
import os

# Create tables
models.Base.metadata.create_all(bind=engine)

# Seed admin if not exists
def seed_admin():
    db = SessionLocal()
    try:
        existing = db.query(models.User).filter(models.User.employee_code == "admin").first()
        if not existing:
            admin_user = models.User(
                employee_code="admin",
                full_name="Quản trị viên",
                role="admin",
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

seed_admin()

app = FastAPI(title="Intern Management API", version="1.0.0")

from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
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
app.include_router(auth_router.router)
app.include_router(user_router.router)
app.include_router(admin_router.router)
app.include_router(schedule_router.router)

# Serve frontend
frontend_dir = os.path.join(os.path.dirname(__file__), "..", "frontend")
if os.path.exists(frontend_dir):
    app.mount("/static", StaticFiles(directory=os.path.join(frontend_dir, "static")), name="static")

    @app.get("/")
    def serve_index():
        return FileResponse(os.path.join(frontend_dir, "index.html"))

    @app.get("/{path:path}")
    def serve_spa(path: str):
        fp = os.path.join(frontend_dir, path)
        if os.path.exists(fp):
            return FileResponse(fp)
        return FileResponse(os.path.join(frontend_dir, "index.html"))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
