import uvicorn
import sys

if __name__ == "__main__":
    if getattr(sys, "frozen", False) or hasattr(sys, 'argv') and sys.argv[0].endswith("__main__.py"):
        from src.main import app

        uvicorn.run(app, port=5000)
    else:
        uvicorn.run("src.main:app", port=5000, reload=True, workers=2)
