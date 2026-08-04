from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from database import get_db
import models
import schemas
from auth import get_current_user

router = APIRouter(prefix="/api/ai-config", tags=["ai_config"])

@router.get("", response_model=schemas.AIConfigResponse)
def get_config(db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    if current_user.role != "admin":
        raise HTTPException(status_code=403, detail="Not authorized")
    
    config = db.query(models.AIConfig).first()
    if not config:
        config = models.AIConfig()
        db.add(config)
        db.commit()
        db.refresh(config)
    
    return config

@router.post("", response_model=schemas.AIConfigResponse)
def update_config(config_in: schemas.AIConfigUpdate, db: Session = Depends(get_db), current_user: models.User = Depends(get_current_user)):
    if current_user.role != "admin":
        raise HTTPException(status_code=403, detail="Not authorized")
    
    config = db.query(models.AIConfig).first()
    if not config:
        config = models.AIConfig()
        db.add(config)
        
    for var, value in vars(config_in).items():
        if value is not None:
            setattr(config, var, value)
            
    db.commit()
    db.refresh(config)
    return config
