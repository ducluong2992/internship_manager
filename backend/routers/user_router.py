from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from database import get_db
import models, schemas, auth

router = APIRouter(prefix="/users", tags=["Users"])


@router.get("/me", response_model=schemas.UserResponse)
def get_me(current_user: models.User = Depends(auth.get_current_user)):
    return current_user


@router.put("/me", response_model=schemas.UserResponse)
def update_me(
    data: schemas.UserUpdate,
    current_user: models.User = Depends(auth.get_current_user),
    db: Session = Depends(get_db),
):
    # Intern can only update personal info fields
    allowed = ["gender", "ethnicity", "birthday", "hometown", "phone", "cccd",
                "bank_name", "bank_account", "viettel_email", "employment_type"]
    for field in allowed:
        val = getattr(data, field, None)
        if val is not None:
            setattr(current_user, field, val)
    db.commit()
    db.refresh(current_user)
    return current_user
