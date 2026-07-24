from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List
from datetime import datetime

from database import get_db
import models, schemas, auth

router = APIRouter(prefix="/schedule", tags=["Schedule"])


@router.get("/periods")
def get_open_periods(db: Session = Depends(get_db), _: models.User = Depends(auth.get_current_user)):
    periods = db.query(models.SchedulePeriod).order_by(
        models.SchedulePeriod.year.desc(), models.SchedulePeriod.month.desc()
    ).all()
    return [
        {
            "id": p.id,
            "month": p.month,
            "year": p.year,
            "status": p.status,
            "open_date": str(p.open_date) if p.open_date else None,
            "close_date": str(p.close_date) if p.close_date else None,
        }
        for p in periods
    ]


@router.get("/me")
def get_my_schedule(
    period_id: int,
    current_user: models.User = Depends(auth.get_current_user),
    db: Session = Depends(get_db),
):
    schedules = (
        db.query(models.Schedule)
        .filter(
            models.Schedule.period_id == period_id,
            models.Schedule.user_id == current_user.id,
        )
        .all()
    )
    return [
        {"id": s.id, "work_day": str(s.work_day), "shift": s.shift}
        for s in schedules
    ]


@router.post("/me")
def submit_schedule(
    data: schemas.ScheduleSubmit,
    current_user: models.User = Depends(auth.get_current_user),
    db: Session = Depends(get_db),
):
    period = db.query(models.SchedulePeriod).filter(models.SchedulePeriod.id == data.period_id).first()
    if not period:
        raise HTTPException(status_code=404, detail="Không tìm thấy kỳ đăng ký")
    if period.status != "open":
        raise HTTPException(status_code=400, detail="Kỳ đăng ký đã đóng")

    # Delete existing then insert fresh
    db.query(models.Schedule).filter(
        models.Schedule.period_id == data.period_id,
        models.Schedule.user_id == current_user.id,
    ).delete()

    for entry in data.entries:
        if entry.shift in ("S", "C", "SC"):
            s = models.Schedule(
                period_id=data.period_id,
                user_id=current_user.id,
                work_day=entry.work_day,
                shift=entry.shift,
            )
            db.add(s)
    db.commit()
    return {"message": "Đăng ký lịch thành công"}
