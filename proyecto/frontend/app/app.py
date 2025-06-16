from flask import Flask, render_template, send_from_directory, url_for, request, redirect, flash, session, jsonify
from flask_login import LoginManager, login_manager, current_user, login_user, login_required, logout_user
import requests
import os
import uuid
import logging
from datetime import datetime

# Usuarios
from models import users, User, Conversation, conversations

# Login
from forms import LoginForm, SignupForm, SettingsForm

app = Flask(__name__, static_url_path='')
login_manager = LoginManager()
login_manager.init_app(app) # Para mantener la sesión
logging.basicConfig(
    filename='app_logs.log',
    level=logging.DEBUG,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
# Configurar el secret_key. OJO, no debe ir en un servidor git público.
# Python ofrece varias formas de almacenar esto de forma segura, que
# no cubriremos aquí.
app.config['SECRET_KEY'] = 'qH1vprMjavek52cv7Lmfe1FoCexrrV8egFnB21jHhkuOHm8hJUe1hwn7pKEZQ1fioUzDb3sWcNK1pJVVIhyrgvFiIrceXpKJBFIn_i9-LTLBCc4cqaI3gjJJHU6kxuT8bnC7Ng'

@app.route('/static/<path:path>')
def serve_static(path):
    return send_from_directory('static', path)

@app.route('/')
def index():
    return render_template('index.html', current_page='index')

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

    return render_template('signup.html', form=form, current_page='signup')

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

    return render_template('login.html', form=form, error=error, current_page='login')

@app.route('/profile')
@login_required
def profile():
    try:
        response = requests.get(f"http://backend-rest:8080/Service/u/{current_user.email}")
        if response.status_code == 200:
            user_data = response.json()
            return render_template('profile.html', user=user_data, current_page='profile')
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

from flask import render_template, redirect, url_for
from flask_login import login_required, current_user
import requests
from datetime import datetime

@app.route('/stats')
@login_required
def stats():
    backend_url = "http://backend-rest:8080/Service"
    r = requests.get(f"{backend_url}/u/{current_user.email}/statistics")

    if r.status_code == 200:
        data = r.json()

        # Limpieza de la fecha
        last_activity_raw = data.get("lastActivity", "").replace("[UTC]", "")
        try:
            last_activity = datetime.fromisoformat(last_activity_raw.replace("Z", "+00:00"))
        except Exception:
            last_activity = None

        # Pasamos todo al template
        return render_template(
            "stats.html",
            numLogins=data.get("numLogins", 0),
            numPrompts=data.get("numPrompts", 0),
            lastActivity=last_activity
        )

    elif r.status_code == 404:
        return render_template("stats.html", error="No hay estadísticas")

    else:
        return render_template("stats.html", error="No se pudieron obtener las estadísticas")


@app.route('/prompt', methods=['GET', 'POST'])
@login_required
def prompt():
    backend_url = "http://backend-rest:8080/Service"
    if request.method == 'GET':
        conversation_name = request.args.get('conversation_name')
        if conversation_name:
            # Recuperar conversación existente
            r = requests.get(f"{backend_url}/u/{current_user.email}/dialogue/{conversation_name}")
            if r.status_code == 200:
                conversation = r.json()
                return render_template('prompt.html', conversation=conversation, current_page='prompt')
            else:
                flash('Conversación no encontrada.', 'danger')
                return redirect(url_for('logs'))
        else:
            # Mostrar formulario para crear conversación (introducir título)
            return render_template('prompt.html', conversation=None, current_page='prompt')

    # POST
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No se recibieron datos'}), 400

    # Crear conversación nueva con título
    title = data.get('title')
    if title: 
    	r = requests.post(f"{backend_url}/u/{current_user.email}/dialogue", json={"name": data['title']})
    	location = r.headers.get('Location')

    	if r.status_code == 201:
            conv = r.json()
            #return jsonify(conv)
            session['conversation_name'] = title
            session['nextUrl'] = conv.get('nextUrl')
            session['endUrl'] = conv.get('endUrl')
            return jsonify({'conversation_name': title})
    	else:
            return jsonify({'error': 'No se recibieron datos'}), 400
    # Añadir mensaje a conversación existente
    user_message = data.get('message')
    nextUrl = session.get('nextUrl')
    if not user_message:
        return jsonify({'error': 'Faltan parámetros user_message'}), 400
    if not nextUrl:
        return jsonify({'error': 'Faltan parámetros nextUrl'}), 400
        
     # POST /u/{email}/dialogue/{name}/next/{nextToken}
    payload = {
        "prompt": user_message,
        "timestamp": datetime.now().isoformat()
    }
    r = requests.post(f"{backend_url}/nextUrl", json=payload)

    if r.status_code == 202:
        return jsonify({'response': f"Respuesta enviada a {user_message}", 'conversation_name': conversation_name})
    elif r.status_code == 400:
        return jsonify({'error': 'Token incorrecto'}), 400
    elif r.status_code == 404:
        return jsonify({'error': 'Conversación no encontrada'}), 404
    else:
        return jsonify({'error': 'Error desconocido al enviar prompt'}), 500
        
    bot_response = f"Respuesta a: {user_message}"

    return jsonify({'response': bot_response, 'conversation_name': conversation_name})


@app.route('/end_conversation', methods=['POST'])
@login_required
def end_conversation():
    session.pop('conversation_id', None)
    return redirect(url_for('prompt'))

@app.route('/logs')
@login_required
def logs():
    user_conversations = [c for c in conversations if c.user_id == current_user.id]
    user_conversations.sort(key=lambda c: c.timestamp, reverse=True)
    return render_template('logs.html', conversations=user_conversations, current_page='logs')


@app.route('/delete_conversation', methods=['POST'])
@login_required
def delete_conversation():
    conversation_id = request.form.get('conversation_id')
    if not conversation_id:
        flash("ID de conversación no proporcionado.", "danger")
        return redirect(url_for('logs'))

    # Buscar conversación del usuario
    conversation = next((c for c in conversations if c.id == conversation_id and c.user_id == current_user.id), None)
    if not conversation:
        flash("Conversación no encontrada o sin permiso para eliminar.", "danger")
        return redirect(url_for('logs'))

    # Eliminar la conversación de la lista (o la base de datos si usas DB)
    conversations.remove(conversation)
    flash("Conversación eliminada correctamente.", "success")
    return redirect(url_for('logs'))

#NO SE USA
@app.route('/continue_conversation', methods=['GET'])
@login_required
def continue_conversation():
    conversation_id = request.args.get('conversation_id')
    if not conversation_id:
        flash("No se especificó conversación.", "warning")
        return redirect(url_for('logs'))  # O a la página de historial

    # Aquí podrías verificar que la conversación exista y pertenezca al usuario
    convo = Conversation.query.filter_by(id=conversation_id, user_id=current_user.id).first()
    if not convo:
        flash("Conversación no encontrada o no tienes permiso.", "danger")
        return redirect(url_for('logs'))

    # Redirigir al prompt para esa conversación
    return redirect(url_for('prompt', conversation_id=conversation_id))

@app.route('/privacy', methods=['GET'])
def privacy():
    return render_template('politica.html')
    
@app.route('/contact', methods=['GET'])
def contact():
    return render_template('politica.html')     

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=int(os.environ.get('PORT', 5010)))
