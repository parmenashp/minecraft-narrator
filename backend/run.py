import uvicorn
from dotenv import load_dotenv


if __name__ == "__main__":
    load_dotenv()
    uvicorn.run("src.main:app", port=5000, reload=True, workers=2)
