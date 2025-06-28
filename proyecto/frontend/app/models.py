from flask_login import UserMixin
import hashlib
from datetime import datetime

users = []
conversations = []
messages = []

class User(UserMixin):

    def __init__(self, id, name, email, password, is_admin=False):
        self.id = id
        self.name = name
        self.email = email
        self.password = hashlib.sha256(password.encode('utf-8')).hexdigest()
        self.is_admin = is_admin

    def set_password(self, password):
        self.password = hashlib.sha256(password.encode('utf-8')).hexdigest()

    def check_password(self, password):
        return self.password == hashlib.sha256(password.encode('utf-8')).hexdigest()

    def get_id(self):
        return self.email

    def get_user(email):
        for user in users:
            if user.email == email:
                return user
        return None

    def __repr__(self):
        return '<User {}>'.format(self.email)

class Conversation:
    def __init__(self, id, user_id, title):
        self.id = id
        self.user_id = user_id
        self.messages = []
        self.timestamp = datetime.now()
        self.name = name
    
    def __repr__(self):
        return f'<Conversation {self.id} - User {self.user_id} - {self.timestamp}>'


class Message:
    def __init__(self, user_message, bot_response):
        self.user_message = user_message
        self.bot_response = bot_response

    def __repr__(self):
        return f"Message (User: {self.user_message}, Bot: {self.bot_response})"

# Función para crear una conversación
def create_conversation(id, user_id):
    c = Conversation(id, user_id)
    conversations.append(c)
    return c

