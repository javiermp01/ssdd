from flask import Flask, render_template, send_from_directory, url_for, request, redirect, flash, session, jsonify
from flask_login import LoginManager, login_manager, current_user, login_user, login_required, logout_user
import requests
import os
import uuid
import logging

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
        payload = {
            'name': form.name.data,
            'email': form.email.data,
            'password': form.password.data
        }

        try:
            headers = {'Content-Type': 'application/json'}
            response = requests.post("http://backend-rest:8080/Service/signup", json=payload, headers=headers)
            if response.status_code == 201:
                flash('Account created successfully! You are now logged in.', 'success')
                user_data = response.json()
                user = User(user_data["id"], user_data["name"], user_data["email"], "")
                login_user(user)
                return redirect(url_for('index'))
            elif response.status_code == 400:
                flash('Email already registered.', 'danger')
                return redirect(url_for('signup'))
            else:
                flash('Something went wrong. Please try again later.', 'danger')
                return redirect(url_for('signup'))
        except requests.exceptions.RequestException as e:
            flash(f"Error: {e}", 'danger')
            return redirect(url_for('signup'))

    return render_template('signup.html', form=form)

@app.route('/login', methods=['GET', 'POST'])
def login():
    if current_user.is_authenticated:
        return redirect(url_for('index'))
    
    error = None
    form = LoginForm(request.form if request.method == 'POST' else None)

    if request.method == "POST" and form.validate():
        payload = {
            'email': form.email.data,
            'password': form.password.data
        }

        try:
            headers = {'Content-Type': 'application/json'}
            response = requests.post("http://backend-rest:8080/Service/checkLogin", json=payload, headers=headers)

            if response.status_code == 200:  # Login exitoso
                user_data = response.json()
                user = User(user_data["id"], user_data["name"], user_data["email"], "")
                login_user(user, remember=form.remember_me.data)
                return redirect(url_for('index'))
            elif response.status_code == 403:
                error = 'Invalid Credentials. Please try again.'
            else:
                error = 'Something went wrong. Please try again later.'
        except requests.exceptions.RequestException as e:
            error = f"Error: {e}"

    return render_template('login.html', form=form, error=error)
"""
@app.route('/recent')
@login_required
def recent():
    return render_template('recent.html')
"""
@app.route('/profile')
@login_required
def profile():
    try:
        response = requests.get(f"http://backend-rest:8080/Service/u/{current_user.email}")
        if response.status_code == 200:
            user_data = response.json()
            return render_template('profile.html', user=user_data)
        else:
            flash('No se pudo obtener la información del perfil.', 'danger')
            return redirect(url_for('index'))
    except Exception as e:
        flash(f'Error al conectar con el backend: {e}', 'danger')
        return redirect(url_for('index'))

@app.route('/settings', methods=['GET', 'POST'])
@login_required
def settings():
    form = SettingsForm()

    if request.method == 'POST' and form.validate_on_submit():
        user = current_user
        old_email = user.email

        payload = {
            "name": form.new_name.data.strip() or user.name,
            "email": form.new_email.data.strip() or user.email,
            "password": form.new_password.data.strip() or None
        }

        try:
            headers = {'Content-Type': 'application/json'}
            response = requests.put(f"http://backend-rest:8080/Service/u/{old_email}", json=payload, headers=headers)

            if response.status_code == 200:
                updated_data = response.json()
                # Crea un nuevo objeto User con los datos actualizados
                updated_user = User(updated_data["id"], updated_data["name"], updated_data["email"], "")
                if payload["password"]:
                    updated_user.set_password(payload["password"])
                login_user(updated_user)  # Refresca la sesión
                flash('Settings updated successfully!', 'success')
                return redirect(url_for('profile'))
            else:
                flash(f'Error updating user: {response.status_code}', 'danger')

        except requests.exceptions.RequestException as e:
            flash(f"Request error: {e}", 'danger')

        return redirect(url_for('settings'))

    return render_template('settings.html', form=form)

@app.route('/delete_account', methods=['POST'])
@login_required
def delete_account():
    try:
        headers = {'Content-Type': 'application/json'}
        print("Intentando borrar usuario:", current_user.email)  # DEBUG
        response = requests.delete(f"http://backend-rest:8080/Service/u/{current_user.email}", headers=headers)
        print("Código de respuesta backend:", response.status_code, response.text)  # DEBUG
        if response.status_code == 200:
            flash('Your account has been deleted.', 'danger')
        else:
            flash('Could not delete your account.', 'danger')
    except requests.exceptions.RequestException as e:
        flash(f"Request error: {e}", 'danger')
    logout_user()
    return redirect(url_for('index'))


@app.route('/logout')
@login_required
def logout():
    logout_user()
    return redirect(url_for('index'))

@login_manager.user_loader
def load_user(user_email):
    try:
        response = requests.get(f"http://backend-rest:8080/Service/u/{user_email}")
        if response.status_code == 200:
            user_data = response.json()
            return User(user_data["id"], user_data["name"], user_data["email"], "")
        else:
            return None
    except Exception:
        return None

@app.route('/prompt', methods=['GET', 'POST'])
@login_required
def prompt():
    logging.debug("Post")
    
    # Si hay un `conversation_id` en la URL, se recupera la conversación correspondiente
    conversation_id = request.args.get('conversation_id')
    
    if conversation_id:
        # Recuperar la conversación activa usando el `conversation_id` de la URL
        conversation = next((c for c in conversations if c.id == conversation_id), None)
        if conversation is None:
            flash('Conversación no encontrada.', 'danger')
            return redirect(url_for('index'))  # O a donde prefieras redirigir
        logging.debug(f"Continuing conversation. ID: {conversation_id}")
    else:
        # Si no hay `conversation_id`, crear una nueva conversación
        if 'conversation_id' not in session:
            conversation_id = str(uuid.uuid4())
            conversation = Conversation(conversation_id, current_user.id)
            conversations.append(conversation)
            session['conversation_id'] = conversation_id
            logging.debug(f"Created new conversation. ID: {conversation_id}")
        else:
            conversation = next((c for c in conversations if c.id == session['conversation_id']), None)
            logging.debug(f"Existing conversation. ID: {session.get('conversation_id')}")
    
    if request.method == 'POST':
        user_message = request.json.get('message')
        if not user_message:
            flash('Empty message.', 'danger')
            return redirect(url_for('prompt'))

        bot_response = f"Respuesta a: {user_message}"  

        # Agregar mensaje a la conversación
        conversation.add_message(user_message, bot_response)

        logging.debug(f"User: {user_message}, Bot: {bot_response}")

        return jsonify({'response': bot_response, 'conversation_id': conversation.id})

    return render_template('prompt.html', conversation=conversation)

@app.route('/end_conversation', methods=['POST'])
@login_required
def end_conversation():
    session.pop('conversation_id', None)
    return redirect(url_for('prompt'))

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
