from flask import Flask, render_template, send_from_directory, url_for, request, redirect, flash
from flask_login import LoginManager, login_manager, current_user, login_user, login_required, logout_user
import requests
import os
import uuid

# Usuarios
from models import users, User, Conversation, conversations

# Login
from forms import LoginForm, SignupForm, SettingsForm

app = Flask(__name__, static_url_path='')
login_manager = LoginManager()
login_manager.init_app(app) # Para mantener la sesión

# Configurar el secret_key. OJO, no debe ir en un servidor git público.
# Python ofrece varias formas de almacenar esto de forma segura, que
# no cubriremos aquí.
app.config['SECRET_KEY'] = 'qH1vprMjavek52cv7Lmfe1FoCexrrV8egFnB21jHhkuOHm8hJUe1hwn7pKEZQ1fioUzDb3sWcNK1pJVVIhyrgvFiIrceXpKJBFIn_i9-LTLBCc4cqaI3gjJJHU6kxuT8bnC7Ng'

@app.route('/static/<path:path>')
def serve_static(path):
    return send_from_directory('static', path)

@app.route('/')
def index():
    return render_template('index.html')

@app.route('/signup', methods=['GET', 'POST'])
def signup():
    form = SignupForm()
    if request.method == 'POST' and form.validate_on_submit():
        existing_user = next((u for u in users if u.email == form.email.data), None)
        if existing_user:
            flash('Email already registered.', 'danger')
            return redirect(url_for('signup'))

        new_user = User(len(users) + 1, form.name.data, form.email.data, form.password.data)
        users.append(new_user)  # Almacenamos el usuario en la lista temporal

        flash('Account created successfully! You can log in now.', 'success')
        return redirect(url_for('login'))

    return render_template('signup.html', form=form)

@app.route('/login', methods=['GET', 'POST'])
def login():
    if current_user.is_authenticated:
        return redirect(url_for('index'))
    
    error = None
    form = LoginForm(request.form if request.method == 'POST' else None)

    if request.method == "POST" and form.validate():
        # Buscar el usuario en la lista
        user = next((u for u in users if u.email == form.email.data), None)

        # Validar usuario y contraseña con el método check_password()
        if user is None or not user.check_password(form.password.data):
            error = 'Invalid Credentials. Please try again.'
        else:
            login_user(user, remember=form.remember_me.data)
            return redirect(url_for('index'))

    return render_template('login.html', form=form, error=error)

@app.route('/recent')
@login_required
def recent():
    return render_template('recent.html')

@app.route('/profile')
@login_required
def profile():
    return render_template('profile.html')

@app.route('/settings', methods=['GET', 'POST'])
@login_required
def settings():
    form = SettingsForm()

    if request.method == 'POST' and form.validate_on_submit():
        user = current_user  # Usuario autenticado

        # Buscar el usuario en `users` y actualizar solo los datos modificados
        for i, u in enumerate(users):
            if u.id == user.id:
                if form.new_name.data.strip():  # Si el campo no está vacío, actualizar
                    users[i].name = form.new_name.data.strip()
                    user.name = form.new_name.data.strip()
                if form.new_email.data.strip():
                    users[i].email = form.new_email.data.strip()
                    user.email = form.new_email.data.strip()
                if form.new_password.data.strip():
                    users[i].set_password(form.new_password.data.strip())
                    user.set_password(form.new_password.data.strip())
                break

        flash('Settings updated successfully!', 'success')
        return redirect(url_for('settings'))

    return render_template('settings.html', form=form)



@app.route('/delete_account', methods=['POST'])
@login_required
def delete_account():
    global users
    users = [u for u in users if u.email != current_user.email]  # Elimina el usuario
    logout_user()
    flash('Your account has been deleted.', 'danger')
    return redirect(url_for('index'))


@app.route('/logout')
@login_required
def logout():
    logout_user()
    return redirect(url_for('index'))

@login_manager.user_loader
def load_user(user_id):
    for user in users:
        if user.id == int(user_id):
            return user
    return None

@app.route('/prompt', methods=['GET', 'POST'])
@login_required
def prompt():
    if request.method == 'POST':
        user_message = request.json.get('message')
        if not user_message:
            return jsonify({'error': 'Mensaje vacío'}), 400

        bot_response = f"Respuesta a: {user_message}"  

        # Obtener la conversación activa del usuario
        conversation = next((c for c in conversations if c.user_id == current_user.id and not c.ended), None)
        
        if not conversation:
            # Crear nueva conversación
            conversation_id = str(uuid.uuid4())
            conversation = Conversation(conversation_id, current_user.id, user_message, bot_response)
            conversation.ended = False
            conversations.append(conversation)
        else:
            # Agregar mensaje a la conversación existente
            conversation.user_message += f"\n{user_message}"
            conversation.bot_response += f"\n{bot_response}"

        return jsonify({'response': bot_response, 'conversation_id': conversation.id})

    return render_template('prompt.html')

@app.route('/end_conversation', methods=['POST'])
@login_required
def end_conversation():
    conversation = next((c for c in conversations if c.user_id == current_user.id and not c.ended), None)
    if conversation:
        conversation.ended = True  # Marcar la conversación como finalizada
    return redirect(url_for('logs'))

@app.route('/logs')
@login_required
def logs():
    #conversations = Conversation.query.filter_by(user_id=current_user.id).order_by(Conversation.timestamp.desc()).all()
    #return render_template('logs.html', conversations=conversations)
    user_conversations = [c for c in conversations if c.user_id == current_user.id]
    user_conversations.sort(key=lambda c: c.timestamp, reverse=True)
    return render_template('logs.html', conversations=user_conversations)


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=int(os.environ.get('PORT', 5010)))
