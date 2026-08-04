from fastapi import APIRouter, Depends, HTTPException, Query, UploadFile, File
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session
from typing import List, Optional
from io import BytesIO
import openpyxl
from openpyxl.styles import Font, PatternFill, Alignment, Border, Side
import calendar
from datetime import date
from pydantic import BaseModel
import urllib.request
import re

from database import get_db
import models, schemas, auth

router = APIRouter(prefix="/admin", tags=["Admin"])


# ─── User Management ──────────────────────────────────────────────────────────

import unicodedata
from datetime import datetime

def remove_accents(input_str):
    s = input_str.replace('đ', 'd').replace('Đ', 'D')
    nfkd_form = unicodedata.normalize('NFKD', s)
    return u"".join([c for c in nfkd_form if not unicodedata.combining(c)])

    return {"employee_code": employee_code, "password": password}


@router.get("/users", response_model=List[schemas.UserResponse])
def list_users(
    db: Session = Depends(get_db),
    _: models.User = Depends(auth.require_admin),
):
    # Only return interns (user_type=intern) for the TTS management page
    return db.query(models.User).filter(
        models.User.user_type == "intern"
    ).order_by(models.User.created_at.desc()).all()


@router.post("/users", response_model=schemas.UserResponse)
def create_user(
    data: schemas.UserCreate,
    db: Session = Depends(get_db),
    _: models.User = Depends(auth.require_admin),
):
    existing = db.query(models.User).filter(models.User.employee_code == data.employee_code).first()
    if existing:
        raise HTTPException(status_code=400, detail="Mã nhân viên đã tồn tại")
    
    # Force user_type=intern for this endpoint
    user_data = data.model_dump()
    user_data["user_type"] = "intern"
    user_data["role"] = "user" if data.role not in ("admin",) else data.role

    # 1. Create User
    user = models.User(**user_data)
    db.add(user)
    db.flush()

    # 2. Generate Username (prefix tts_)
    name_clean = remove_accents(data.full_name).lower()
    parts = name_clean.split()
    if len(parts) == 0:
        base_username = "tts_user"
    elif len(parts) == 1:
        base_username = f"tts_{parts[0]}"
    else:
        first_name = parts[-1]
        initials = "".join([p[0] for p in parts[:-1]])
        base_username = f"tts_{first_name}{initials}"
    
    username = base_username
    suffix = 1
    while db.query(models.Account).filter(models.Account.username == username).first():
        username = f"{base_username}{suffix}"
        suffix += 1

    # 3. Create Account
    account = models.Account(
        user_id=user.id,
        username=username,
        password=auth.hash_password("123456")
    )
    db.add(account)
    db.commit()
    db.refresh(user)
    return user


@router.put("/users/{user_id}", response_model=schemas.UserResponse)
def update_user(
    user_id: int,
    data: schemas.UserUpdate,
    db: Session = Depends(get_db),
    _: models.User = Depends(auth.require_admin),
):
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Không tìm thấy người dùng")
    for field, val in data.model_dump(exclude_unset=True).items():
        setattr(user, field, val)
    db.commit()
    db.refresh(user)
    return user


@router.delete("/users/{user_id}")
def delete_user(
    user_id: int,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(auth.require_admin),
):
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Không tìm thấy người dùng")
    if user.id == current_user.id:
        raise HTTPException(status_code=400, detail="Không thể xóa tài khoản đang đăng nhập")
    # Delete related schedules first
    db.query(models.Schedule).filter(models.Schedule.user_id == user_id).delete()
    db.delete(user)
    db.commit()
    return {"message": f"Đã xóa tài khoản {user.full_name}"}


from fastapi import UploadFile, File
import pandas as pd # Actually openpyxl is already imported, we'll use openpyxl directly
@router.get("/users/import-template")
def download_import_template(
    _: models.User = Depends(auth.require_admin),
):
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Danh sách Thực tập sinh"
    
    headers = [
        "MÃ NHÂN VIÊN (*)", "HỌ VÀ TÊN (*)", "GIỚI TÍNH", "NGÀY SINH (YYYY-MM-DD)",
        "DÂN TỘC", "CCCD", "SĐT", "QUÊ QUÁN",
        "NGÂN HÀNG", "SỐ TÀI KHOẢN", 
        "LOẠI NHÂN SỰ (TTS TRUNG TÂM/ĐI MƯỢN)", "LOẠI HÌNH (FULLTIME/PARTTIME)",
        "VAI TRÒ (ADMIN/INTERN)", "NGÀY VÀO LÀM (YYYY-MM-DD)", "VỊ TRÍ (BA/DEV/...)"
    ]
    
    header_font = Font(bold=True)
    header_fill = PatternFill("solid", fgColor="DDEEFF")
    center = Alignment(horizontal="center", vertical="center")
    
    for col_idx, header in enumerate(headers, start=1):
        c = ws.cell(1, col_idx, header)
        c.font = header_font
        c.fill = header_fill
        c.alignment = center
        ws.column_dimensions[c.column_letter].width = 20
        
    buf = BytesIO()
    wb.save(buf)
    buf.seek(0)
    return StreamingResponse(
        buf,
        media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers={"Content-Disposition": "attachment; filename=template_import_tts.xlsx"},
    )


@router.post("/users/import")
def import_users(
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    _: models.User = Depends(auth.require_admin),
):
    if not file.filename.endswith('.xlsx'):
        raise HTTPException(status_code=400, detail="Chỉ hỗ trợ định dạng .xlsx")
        
    try:
        content = file.file.read()
        wb = openpyxl.load_workbook(filename=BytesIO(content), data_only=True)
        ws = wb.active
    except Exception:
        raise HTTPException(status_code=400, detail="Không thể đọc file Excel")
        
    return process_import_users(ws, db)

class ImportLinkRequest(BaseModel):
    url: str

@router.post("/users/import-link")
def import_users_from_link(
    data: ImportLinkRequest,
    db: Session = Depends(get_db),
    _: models.User = Depends(auth.require_admin),
):
    match = re.search(r'/d/([a-zA-Z0-9-_]+)', data.url)
    if not match:
        raise HTTPException(status_code=400, detail="Đường dẫn Google Sheets không hợp lệ")
    sheet_id = match.group(1)
    export_url = f"https://docs.google.com/spreadsheets/d/{sheet_id}/export?format=xlsx"
    
    try:
        req = urllib.request.Request(export_url)
        with urllib.request.urlopen(req) as response:
            content = response.read()
            wb = openpyxl.load_workbook(filename=BytesIO(content), data_only=True)
            ws = wb.active
    except Exception:
        raise HTTPException(status_code=400, detail="Không thể tải hoặc đọc dữ liệu từ link. Hãy chắc chắn link đã được chia sẻ công khai 'Bất kỳ ai có liên kết'.")
        
    return process_import_users(ws, db)

def process_import_users(ws, db: Session):
    # Mapping Excel columns to User fields
    # 0: Mã nhân viên, 1: Họ tên, 2: Giới tính, 3: Ngày sinh, 4: Dân tộc, 5: CCCD
    # 6: SĐT, 7: Email, 8: Quê quán, 9: Ngân hàng, 10: Số TK, 11: Dự án
    # 12: Trợ cấp, 13: Loại nhân sự, 14: Loại hình
    
    success_count = 0
    skip_count = 0
    
    for row in ws.iter_rows(min_row=2, values_only=True):
        if not row or not row[0] or not row[1]:
            continue # Skip empty rows or rows without mandatory fields
            
        emp_code = str(row[0]).strip()
        full_name = str(row[1]).strip()
        
        existing = db.query(models.User).filter(models.User.employee_code == emp_code).first()
        if existing:
            skip_count += 1
            continue
            
        # Parse date
        birthday = None
        if row[3]:
            if isinstance(row[3], datetime):
                birthday = row[3].date()
            elif isinstance(row[3], date):
                birthday = row[3]
            elif isinstance(row[3], str):
                try:
                    birthday = datetime.strptime(row[3].strip(), "%Y-%m-%d").date()
                except ValueError:
                    pass
                    
        # Parse join_date
        join_date = None
        if len(row) > 13 and row[13]:
            if isinstance(row[13], datetime):
                join_date = row[13].date()
            elif isinstance(row[13], date):
                join_date = row[13]
            elif isinstance(row[13], str):
                try:
                    join_date = datetime.strptime(row[13].strip(), "%Y-%m-%d").date()
                except ValueError:
                    pass
                    
        # Parse integers/strings safely
        allowance = 0
        project = None
        evaluation = None
                
        role = "intern"
        if len(row) > 12 and row[12]:
            r_str = str(row[12]).strip().lower()
            if r_str in ["admin", "quản trị viên", "quản trị"]:
                role = "admin"
                
        position = str(row[14]).strip() if len(row) > 14 and row[14] else None
            
        user_data = {
            "employee_code": emp_code,
            "full_name": full_name,
            "gender": str(row[2]).strip() if row[2] else None,
            "birthday": birthday,
            "ethnicity": str(row[4]).strip() if len(row) > 4 and row[4] else None,
            "cccd": str(row[5]).strip() if len(row) > 5 and row[5] else None,
            "phone": str(row[6]).strip() if len(row) > 6 and row[6] else None,
            "viettel_email": None,
            "hometown": str(row[7]).strip() if len(row) > 7 and row[7] else None,
            "bank_name": str(row[8]).strip() if len(row) > 8 and row[8] else None,
            "bank_account": str(row[9]).strip() if len(row) > 9 and row[9] else None,
            "project": project,
            "position": position,
            "join_date": join_date,
            "evaluation": evaluation,
            "allowance": allowance,
            "employee_type": str(row[10]).strip() if len(row) > 10 and row[10] else "TTS Trung tâm",
            "employment_type": str(row[11]).strip() if len(row) > 11 and row[11] else "Fulltime",
            "role": role,
            "working_status": "Working",
            "account_status": 1
        }
        
        user = models.User(**user_data)
        db.add(user)
        db.flush()
        
        # Generate Username
        name_clean = remove_accents(full_name).lower()
        parts = name_clean.split()
        if len(parts) == 0:
            base_username = "tts_user"
        elif len(parts) == 1:
            base_username = f"tts_{parts[0]}"
        else:
            first_name = parts[-1]
            initials = "".join([p[0] for p in parts[:-1]])
            base_username = f"tts_{first_name}{initials}"
        
        username = base_username
        suffix = 1
        while db.query(models.Account).filter(models.Account.username == username).first():
            username = f"{base_username}{suffix}"
            suffix += 1
            
        account = models.Account(
            user_id=user.id,
            username=username,
            password=auth.hash_password("123456")
        )
        db.add(account)
        success_count += 1
        
    db.commit()
    return {"message": f"Đã nhập {success_count} thực tập sinh thành công. Bỏ qua {skip_count} người (trùng mã).", "success": success_count, "skipped": skip_count}


@router.patch("/users/{user_id}/lock")
def toggle_lock(
    user_id: int,
    db: Session = Depends(get_db),
    _: models.User = Depends(auth.require_admin),
):
    user = db.query(models.User).filter(models.User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="Không tìm thấy người dùng")
    user.account_status = 0 if user.account_status == 1 else 1
    db.commit()
    return {"account_status": user.account_status, "message": "Đã cập nhật trạng thái tài khoản"}


@router.patch("/users/{user_id}/reset-password")
def reset_password(
    user_id: int,
    db: Session = Depends(get_db),
    _: models.User = Depends(auth.require_admin),
):
    account = db.query(models.Account).filter(models.Account.user_id == user_id).first()
    if not account:
        raise HTTPException(status_code=404, detail="Không tìm thấy tài khoản cho người dùng này")
    account.password = auth.hash_password("123456")
    db.commit()
    return {"message": f"Đã đặt lại mật khẩu về 123456 cho tài khoản {account.username}"}


# ─── Stats ────────────────────────────────────────────────────────────────────

@router.get("/accounts", response_model=List[schemas.AdminAccountRow])
def list_accounts(
    user_type: str = Query("intern"),
    db: Session = Depends(get_db),
    _: models.User = Depends(auth.require_admin),
):
    users = db.query(models.User).filter(models.User.user_type == user_type).order_by(models.User.created_at.desc()).all()
    results = []
    for u in users:
        results.append(schemas.AdminAccountRow(
            user_id=u.id,
            employee_code=u.employee_code,
            full_name=u.full_name,
            username=u.account.username if u.account else None,
            account_status=u.account_status,
        ))
    return results


@router.get("/accounts/export")
def export_accounts(
    user_type: str = Query("intern"),
    db: Session = Depends(get_db),
    _: models.User = Depends(auth.require_admin),
):
    users = db.query(models.User).filter(models.User.user_type == user_type).order_by(models.User.full_name).all()
    
    wb = openpyxl.Workbook()
    ws = wb.active
    ws.title = "Danh sach Tai khoan"

    header_font = Font(color="FFFFFF", bold=True)
    header_fill = PatternFill("solid", fgColor="1e3a5f")
    center = Alignment(horizontal="center", vertical="center")
    
    headers = ["STT", "Mã NV", "Họ tên", "Tên đăng nhập", "Trạng thái"]
    for col_idx, header in enumerate(headers, start=1):
        c = ws.cell(1, col_idx, header)
        c.font = header_font
        c.fill = header_fill
        c.alignment = center
    
    ws.column_dimensions["B"].width = 15
    ws.column_dimensions["C"].width = 25
    ws.column_dimensions["D"].width = 20
    ws.column_dimensions["E"].width = 15

    for row_idx, u in enumerate(users, start=2):
        ws.cell(row_idx, 1, row_idx - 1).alignment = center
        ws.cell(row_idx, 2, u.employee_code).alignment = center
        ws.cell(row_idx, 3, u.full_name)
        ws.cell(row_idx, 4, u.account.username if u.account else "N/A").alignment = center
        ws.cell(row_idx, 5, "Mở" if u.account_status == 1 else "Khóa").alignment = center

    buf = BytesIO()
    wb.save(buf)
    buf.seek(0)
    return StreamingResponse(
        buf,
        media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers={"Content-Disposition": "attachment; filename=danh_sach_tai_khoan.xlsx"},
    )

@router.get("/stats")
def get_stats(
    db: Session = Depends(get_db),
    _: models.User = Depends(auth.require_admin),
):
    from datetime import datetime
    now = datetime.now()
    interns = db.query(models.User).filter(models.User.user_type == "intern").all()
    employees = db.query(models.User).filter(models.User.user_type == "employee").all()
    periods = db.query(models.SchedulePeriod).all()
    open_periods = [p for p in periods if p.status == "open"]
    working = [u for u in interns if (u.working_status or '').lower() == "working"]
    resigned = [u for u in interns if (u.working_status or '').lower() == "resigned"]
    fulltime = [u for u in interns if (u.employment_type or '').lower() == "fulltime"]
    parttime = [u for u in interns if (u.employment_type or '').lower() == "parttime"]
    intern_count = [u for u in interns if (u.employee_type or '').lower() in ["tts trung tâm", "intern", "thực tập"]]
    borrowed_count = [u for u in interns if (u.employee_type or '').lower() in ["đi mượn", "borrowed"]]
    
    emp_trung_tam = [u for u in employees if (u.staff_category or '').lower() in ["ns trung tâm", "nhân sự trung tâm"]]
    emp_cho_muon = [u for u in employees if (u.staff_category or '').lower() in ["cho mượn", "đi mượn"]]
    emp_onsite = [u for u in employees if (u.staff_category or '').lower() in ["onsite"]]
    
    today_date = now.date()
    today_schedules = (
        db.query(models.Schedule, models.User)
        .join(models.User, models.Schedule.user_id == models.User.id)
        .filter(models.Schedule.work_day == today_date)
        .all()
    )
    today_workers = [
        {"employee_code": u.employee_code, "full_name": u.full_name, "shift": s.shift}
        for s, u in today_schedules if s.shift in ("S", "C", "SC") and (u.working_status or '').lower() == "working"
    ]
    
    return {
        "total_interns": len(interns),
        "total_employees": len(employees),
        "working": len(working),
        "resigned": len(resigned),
        "fulltime": len(fulltime),
        "parttime": len(parttime),
        "intern_count": len(intern_count),
        "borrowed_count": len(borrowed_count),
        "emp_trung_tam": len(emp_trung_tam),
        "emp_cho_muon": len(emp_cho_muon),
        "emp_onsite": len(emp_onsite),
        "total_periods": len(periods),
        "open_periods": len(open_periods),
        "today_workers": today_workers,
    }


# ─── Schedule Period Management ───────────────────────────────────────────────

@router.get("/periods", response_model=List[schemas.PeriodResponse])
def list_periods(
    db: Session = Depends(get_db),
    _: models.User = Depends(auth.require_admin),
):
    return db.query(models.SchedulePeriod).order_by(
        models.SchedulePeriod.year.desc(), models.SchedulePeriod.month.desc()
    ).all()


@router.post("/periods", response_model=schemas.PeriodResponse)
def create_period(
    data: schemas.PeriodCreate,
    db: Session = Depends(get_db),
    _: models.User = Depends(auth.require_admin),
):
    existing = db.query(models.SchedulePeriod).filter(
        models.SchedulePeriod.month == data.month,
        models.SchedulePeriod.year == data.year,
    ).first()
    if existing:
        raise HTTPException(status_code=400, detail="Kỳ đăng ký tháng này đã tồn tại")
    period = models.SchedulePeriod(**data.model_dump())
    db.add(period)
    db.commit()
    db.refresh(period)
    return period


@router.put("/periods/{period_id}", response_model=schemas.PeriodResponse)
def update_period(
    period_id: int,
    data: schemas.PeriodUpdate,
    db: Session = Depends(get_db),
    _: models.User = Depends(auth.require_admin),
):
    period = db.query(models.SchedulePeriod).filter(models.SchedulePeriod.id == period_id).first()
    if not period:
        raise HTTPException(status_code=404, detail="Không tìm thấy kỳ đăng ký")
    for field, val in data.model_dump(exclude_unset=True).items():
        setattr(period, field, val)
    db.commit()
    db.refresh(period)
    return period


@router.delete("/periods/{period_id}")
def delete_period(
    period_id: int,
    db: Session = Depends(get_db),
    _: models.User = Depends(auth.require_admin),
):
    period = db.query(models.SchedulePeriod).filter(models.SchedulePeriod.id == period_id).first()
    if not period:
        raise HTTPException(status_code=404, detail="Không tìm thấy kỳ đăng ký")
    db.query(models.Schedule).filter(models.Schedule.period_id == period_id).delete()
    db.delete(period)
    db.commit()
    return {"message": "Đã xóa kỳ đăng ký"}


# ─── Admin Schedule View ──────────────────────────────────────────────────────

@router.get("/schedule")
def admin_view_schedule(
    month: int = Query(...),
    year: int = Query(...),
    db: Session = Depends(get_db),
    _: models.User = Depends(auth.require_admin),
):
    period = db.query(models.SchedulePeriod).filter(
        models.SchedulePeriod.month == month,
        models.SchedulePeriod.year == year,
    ).first()

    # Get ALL active interns
    all_interns = db.query(models.User).filter(
        models.User.user_type == "intern",
        models.User.working_status == "Working",
    ).order_by(models.User.employee_code).all()

    if not period:
        return {"period": None, "rows": []}

    schedules = (
        db.query(models.Schedule)
        .filter(models.Schedule.period_id == period.id)
        .all()
    )

    # Build schedule map per user
    sched_by_user = {}
    for s in schedules:
        if s.user_id not in sched_by_user:
            sched_by_user[s.user_id] = []
        sched_by_user[s.user_id].append({
            "id": s.id,
            "work_day": str(s.work_day),
            "shift": s.shift,
        })

    result = []
    for user in all_interns:
        user_scheds = sched_by_user.get(user.id, [])
        total = sum(
            1 if s["shift"] == "SC" else 0.5 if s["shift"] in ("S", "C") else 0
            for s in user_scheds
        )
        result.append({
            "user_id": user.id,
            "employee_code": user.employee_code,
            "full_name": user.full_name,
            "project": user.project or "",
            "total_sessions": total,
            "schedules": user_scheds,
        })

    return {
        "period": {
            "id": period.id,
            "month": period.month,
            "year": period.year,
            "status": period.status,
            "open_date": str(period.open_date) if period.open_date else None,
            "close_date": str(period.close_date) if period.close_date else None,
        },
        "rows": result,
    }


# ─── Export Excel ─────────────────────────────────────────────────────────────

@router.get("/schedule/export")
def export_schedule(
    month: int = Query(...),
    year: int = Query(...),
    db: Session = Depends(get_db),
    _: models.User = Depends(auth.require_admin),
):
    period = db.query(models.SchedulePeriod).filter(
        models.SchedulePeriod.month == month,
        models.SchedulePeriod.year == year,
    ).first()

    wb = openpyxl.Workbook()
    ws = wb.active
    # ws.title = f"Lịch T{month}/{year}"
    ws.title = f"Lịch T{month}-{year}"

    # ── Styles ──
    header_fill  = PatternFill("solid", fgColor="1e3a5f")
    header_font  = Font(color="FFFFFF", bold=True, name="Calibri", size=10)
    center       = Alignment(horizontal="center", vertical="center", wrap_text=True)
    left_align   = Alignment(horizontal="left", vertical="center")
    thin         = Border(
        left=Side(style="thin"), right=Side(style="thin"),
        top=Side(style="thin"), bottom=Side(style="thin")
    )
    sc_fill      = PatternFill("solid", fgColor="C6EFCE")   # green
    half_fill    = PatternFill("solid", fgColor="FFEB9C")   # yellow
    total_fill   = PatternFill("solid", fgColor="DDEEFF")   # light blue
    title_font   = Font(bold=True, size=13, name="Calibri")
    total_font   = Font(bold=True, name="Calibri")

    days_in_month = calendar.monthrange(year, month)[1]
    all_days  = [date(year, month, d) for d in range(1, days_in_month + 1)]
    workdays  = [d for d in all_days if d.weekday() < 5]
    DOW_VN    = ["T2", "T3", "T4", "T5", "T6", "T7", "CN"]

    # ── Title row ──
    ws.merge_cells(start_row=1, start_column=1, end_row=1, end_column=len(workdays) + 3)
    title_cell = ws.cell(1, 1, f"BẢNG LỊCH THỰC TẬP – THÁNG {month}/{year}")
    title_cell.font = title_font
    title_cell.alignment = center
    title_cell.fill = PatternFill("solid", fgColor="E8F0FE")
    ws.row_dimensions[1].height = 24

    # ── Header row (row 2) ──
    def hdr(row, col, val):
        c = ws.cell(row, col, val)
        c.font = header_font
        c.fill = header_fill
        c.alignment = center
        c.border = thin
        return c

    hdr(2, 1, "Mã NV")
    hdr(2, 2, "Họ và tên")
    for col_idx, d in enumerate(workdays, start=3):
        hdr(2, col_idx, f"{d.day}\n{DOW_VN[d.weekday()]}")
        ws.column_dimensions[ws.cell(2, col_idx).column_letter].width = 4.5
    hdr(2, len(workdays) + 3, "Tổng buổi")
    ws.row_dimensions[2].height = 32
    ws.column_dimensions["A"].width = 12
    ws.column_dimensions["B"].width = 24
    col_total_letter = ws.cell(2, len(workdays) + 3).column_letter
    ws.column_dimensions[col_total_letter].width = 10

    if not period:
        buf = BytesIO()
        wb.save(buf); buf.seek(0)
        return StreamingResponse(buf,
            media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            headers={"Content-Disposition": f"attachment; filename=lich_t{month}_{year}.xlsx"})

    schedules = db.query(models.Schedule).filter(models.Schedule.period_id == period.id).all()
    users = (
        db.query(models.User)
        .filter(models.User.user_type == "intern", models.User.working_status == "Working")
        .order_by(models.User.employee_code)
        .all()
    )
    day_col = {d: i + 3 for i, d in enumerate(workdays)}

    grand_total = 0
    for row_idx, user in enumerate(users, start=3):
        ws.cell(row_idx, 1, user.employee_code).border = thin
        ws.cell(row_idx, 1).alignment = center
        name_cell = ws.cell(row_idx, 2, user.full_name)
        name_cell.border = thin
        name_cell.alignment = left_align

        user_sched = {s.work_day: s.shift for s in schedules if s.user_id == user.id}
        total = 0
        for d in workdays:
            col = day_col[d]
            shift = user_sched.get(d, "")
            cell = ws.cell(row_idx, col, shift or "")
            cell.alignment = center
            cell.border = thin
            if shift == "SC":
                cell.fill = sc_fill
                cell.font = Font(bold=True, color="276221")
                total += 1
            elif shift in ("S", "C"):
                cell.fill = half_fill
                cell.font = Font(bold=True, color="9C5700")
                total += 0.5

        grand_total += total
        tc = ws.cell(row_idx, len(workdays) + 3, total)
        tc.alignment = center
        tc.border = thin
        tc.font = total_font
        tc.fill = total_fill

    # ── Grand total row ──
    if users:
        gr = len(users) + 3
        ws.cell(gr, 1, "TỔNG CỘNG").font = Font(bold=True, name="Calibri")
        ws.cell(gr, 1).alignment = center
        ws.cell(gr, 1).border = thin
        ws.merge_cells(start_row=gr, start_column=1, end_row=gr, end_column=len(workdays) + 2)
        for col in range(1, len(workdays) + 3):
            ws.cell(gr, col).fill = PatternFill("solid", fgColor="F2F2F2")
            ws.cell(gr, col).border = thin
        gtc = ws.cell(gr, len(workdays) + 3, grand_total)
        gtc.font = Font(bold=True, size=12, name="Calibri", color="C00000")
        gtc.alignment = center
        gtc.border = thin
        gtc.fill = PatternFill("solid", fgColor="FFE4E1")

    # Freeze header
    ws.freeze_panes = "C3"

    buf = BytesIO()
    wb.save(buf); buf.seek(0)
    return StreamingResponse(
        buf,
        media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers={"Content-Disposition": f"attachment; filename=lich_t{month}_{year}.xlsx"},
    )
