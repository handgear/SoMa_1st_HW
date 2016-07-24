from flask import Flask, Response, request, json
from flask.ext.sqlalchemy import SQLAlchemy

app = Flask(__name__)
app.config["DEBUG"] = True

SQLALCHEMY_DATABASE_URI = "mysql+mysqlconnector://{username}:{password}@{hostname}/{databasename}".format(
    username="handgear",
    password="mysqlokok00",
    hostname="handgear.mysql.pythonanywhere-services.com",
    databasename="handgear$memo_app_db",
)
app.config["SQLALCHEMY_DATABASE_URI"] = SQLALCHEMY_DATABASE_URI
app.config["SQLALCHEMY_POOL_RECYCLE"] = 299

db = SQLAlchemy(app)

class MemoData(db.Model):

    __tablename__ = "memo3"

    id = db.Column(db.Integer, primary_key=True)
    # userId = db.Column(db.String(4096))
    memoId = db.Column(db.Integer)
    memo = db.Column(db.String(4096))
    date = db.Column(db.Date)

    # def __init__(self, userId, memoId, memo, date):
    #     self.userId = userId
    #     self.memoId = memoId
    #     self.memo = memo
    #     self.date = date
    def __init__(self, memoId, memo, date):
        self.memoId = memoId
        self.memo = memo
        self.date = date

@app.route('/api/update', methods = ["POST"])
def api_message():

    if request.headers['Content-Type'] == 'text/plain':
        return "Text Message: " + request.data

    elif request.headers['Content-Type'] == 'application/json':
        

        json_dict = request.get_json()
        # userId = json_dict['userId']
        memoId_ = json_dict['memoId']
        memo_ = json_dict['memo']
        date_ = json_dict['date']

        # t = Comment(json.dumps(request.json))
        # t = Comment(json.dumps(request.json))
        # t = Comment("test auto_inc")
        # db.session.add(MemoData(userId, memoId, memo,date))
        db.session.add(MemoData(memoId_, memo_, date_))
        db.session.commit()
        db.session.close()
        return "JSON Message return by server: " + json.dumps(request.json)

    else:
        return "415 Unsupported Media Type ;)"



#=======================Test func=========================
@app.route('/')
def hello_world():
    return 'Hello from Flask!2'

@app.route('/api/test', methods=['GET', 'POST'])
def hello_world_api():
    if request.method == 'GET':
        result = 'Test! GET!'
    else:
        result = 'Test! GET! else'
    return result

