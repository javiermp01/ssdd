from flask import Flask, render_template, send_from_directory, url_for, request, redirect, flash, session, jsonify
from flask_login import LoginManager, login_manager, current_user, login_user, login_required, logout_user
import requests
import os
import uuid
import logging
import time
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
backend_url = "http://backend-rest:8080"

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

@app.route('/stats')
@login_required
def stats():
    r = requests.get(f"{backend_url}/Service/u/{current_user.email}/statistics")

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
            lastActivity=last_activity,
            current_page='stats'
        )

    elif r.status_code == 404:
        return render_template("stats.html", error="No hay estadísticas")

    else:
        return render_template("stats.html", error="No se pudieron obtener las estadísticas")

def is_name_used(new_name):
    # Obtener nombres de las conversaciones
    r = requests.get(f"{backend_url}/Service/u/{current_user.email}/dialogue")
    if r.status_code != 200:
        return jsonify({'error': "Hubo un problema obteniendo las conversaciones"}), 404

    conversation_names = r.json()
    
    for name in conversation_names:
        if name == new_name:
            return True
    return False

@app.route('/prompt', methods=['GET', 'POST'])
@login_required
def prompt():
    #Cargar conversación si existe o lanzar formulario de creación
    if request.method == 'GET':
        conversation_name = request.args.get('conversation_name')
        if conversation_name:
            r = requests.get(f"{backend_url}/Service/u/{current_user.email}/dialogue/{conversation_name}")
            if r.status_code == 200:
                conv = r.json()
                return render_template('prompt.html', conversation=conv, current_page='prompt')
            else:
                flash('Conversación no encontrada.', 'danger')
                return jsonify({'error': conversation_name}), 500
        else:
            # Mostrar formulario para nueva conversación
            return render_template('create_conv.html', current_page='prompt')
    #Crear conversación
    elif request.method == 'POST':
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No se recibieron datos'}), 400

        conversation_name = data.get('conversation_name')
        if not conversation_name:
            return jsonify({'error': 'Falta el título para crear la conversación'}), 400
            
        if is_name_used(conversation_name):  
            return jsonify({'error': 'Ese nombre ya está en uso'}), 409
            
        r = requests.post(f"{backend_url}/Service/u/{current_user.email}/dialogue", json={"name": conversation_name})
        if r.status_code == 201:
            return jsonify({'conversation_name': conversation_name}), 201
        else:
            return jsonify({'error': 'No se pudo crear la conversación'}), 400

@app.route('/send_prompt', methods=['POST'])
@login_required
def send_prompt():
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No se recibieron datos'}), 400

    user_message = data.get('message')
    conversation_name = data.get('conversation_name')

    if not user_message or not conversation_name:
        return jsonify({'error': 'Faltan parámetros requeridos'}), 400

    r_conv = requests.get(f"{backend_url}/Service/u/{current_user.email}/dialogue/{conversation_name}")
    if r_conv.status_code != 200:
        return jsonify({'error': 'Conversación no encontrada'}), 404

    conv_data = r_conv.json()
    next_url = conv_data.get('nextUrl')
    if not next_url:
        return jsonify({'error': 'No se encontró next URL'}), 400

    payload = {
        "prompt": user_message,
        "timestamp": int(time.time() * 1000)
    }
    
    try:
        # POST inicial para enviar prompt
        r = requests.post(f"{backend_url}/Service{next_url}", json=payload)
        
        if r.status_code == 102:
            return jsonify({"error": "Backend aún no está listo"}), 503
        
        elif r.status_code == 202:
            location_url = r.headers.get('Location')
            if not location_url:
                return jsonify({"error": "No se recibió Location para polling"}), 500

            # Polling haciendo GET a location_url
            for _ in range(15):  # 10 intentos, 1 segundo de espera cada uno
                time.sleep(1)
                poll_response = requests.get(f"{location_url}")
                
                if poll_response.status_code == 200:
                    result = poll_response.json()
                    if result.get("status") == "READY":
                            dialogue = result.get("dialogue", [])
                            if dialogue:
                                last_entry = dialogue[-1]
                                return jsonify({
                                    "response": last_entry.get("response", "Sin respuesta")
                                }), 200
                            else:
                                return jsonify({"error": "Diálogo vacío"}), 500
                    else:
                        continue # No listo aún, seguir esperando
                
                elif poll_response.status_code == 404:
                    return jsonify({'error': 'Conversación no encontrada durante polling'}), 404
                
                else:
                    return jsonify({"error": f"Error inesperado durante polling: {poll_response.status_code}"}), 500
            
            return jsonify({"error": "Timeout esperando respuesta"}), 504
        
        elif r.status_code == 400:
            return jsonify({'error': 'Token incorrecto o solicitud inválida'}), 400
        
        elif r.status_code == 404:
            return jsonify({'error': 'Conversación no encontrada'}), 404
        
        else:
            return jsonify({"error": f"Error al enviar prompt, status {next_url}"}), r.status_code
    
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route('/end_conversation', methods=['POST'])
@login_required
def end_conversation():
    data = request.get_json()
    conversation_name = data.get('conversation_name') if data else None
    if not conversation_name:
        return jsonify({"error": "No se proporcionó el nombre de la conversación"}), 400

    url = f"{backend_url}/Service/u/{current_user.email}/dialogue/{conversation_name}/end"
    try:
        res = requests.post(url)
        if res.status_code == 200:
            return jsonify({"message": "Conversación finalizada correctamente"}), 200
        elif res.status_code == 404:
            return jsonify({"error": f"{backend_url}/Service/u/{current_user.email}/dialogue/{conversation_name}/end"}), res.status_code
        elif res.status_code == 204:
            return jsonify({"error": "La conversación está en estado READY."}), res.status_code
        elif res.status_code == 400:
            return jsonify({"error": "No se puede finalizar la conversación."}), res.status_code
        else:
            return jsonify({"error": "No se pudo finalizar la conversación"}), res.status_code
    except requests.exceptions.RequestException as e:
        flash(f"Error de conexión: {e}", "danger")
        
@app.route('/logs', methods=['GET'])
@login_required
def logs():
    # Obtener nombres de las conversaciones
    r = requests.get(f"{backend_url}/Service/u/{current_user.email}/dialogue")
    if r.status_code != 200:
        return jsonify({'error': "Hubo un problema obteniendo las conversaciones"}), 404

    conversation_names = r.json()
    user_conversations = []

    # Obtener detalles de cada conversación
    for name in conversation_names:
        r_conv = requests.get(f"{backend_url}/Service/u/{current_user.email}/dialogue/{name}")
        if r_conv.status_code == 200:
            convo_data = r_conv.json()
            created_at = convo_data.get("createdAt")

            # Convertir timestamp a fecha legible
            if created_at:
                dt = datetime.fromtimestamp(created_at / 1000)
                created_at_str = dt.strftime("%Y-%m-%d")
            else:
                created_at_str = "Desconocida"

            user_conversations.append({
                "name": name,
                "created_at": created_at,
                "created_at_str": created_at_str
            })

    # Ordenar por timestamp
    user_conversations.sort(key=lambda c: c.get('created_at', 0), reverse=True)

    return render_template('logs.html', conversations=user_conversations, current_page='logs')

@app.route('/delete_conversation', methods=['POST'])
@login_required
def delete_conversation():
    conversation_name = request.form.get('conversation_name')
    if not conversation_name:
        flash("Nombre de conversación no proporcionado.", "danger")
        return redirect(url_for('logs')) 
    
    # Llamar al endpoint REST DELETE para eliminar la conversación
    r = requests.delete(f"{backend_url}/Service/u/{current_user.email}/dialogue/logs/delete/{conversation_name}")

    if r.status_code == 200:
        flash("Conversación eliminada correctamente.", "success")
    elif r.status_code == 404:
        flash("Conversación no encontrada.", "warning")
    else:
        flash(f"Error al eliminar la conversación: {r.status_code}", "danger")

    return redirect(url_for('logs'))

@app.route('/privacy', methods=['GET'])
def privacy():
    return render_template('politica.html')
    
@app.route('/contact', methods=['GET'])
def contact():
    return render_template('politica.html')     

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=int(os.environ.get('PORT', 5010)))
