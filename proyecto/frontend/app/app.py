from flask import Flask, render_template, send_from_directory, url_for, request, redirect, flash
from flask_login import LoginManager, login_manager, current_user, login_user, login_required, logout_user
import requests
import os

# Usuarios
from models import users, User

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
        # Enviar solicitud al backend para registrar el usuario
        payload = {
            'name': form.name.data,
            'email': form.email.data,
            'password': form.password.data  # Asegúrate de que la contraseña se maneje de manera segura
        }

        try:
            headers = {
                'Content-Type': 'application/json',
            }
            response = requests.post("http://backend-rest:8080/Service/signup", json=payload, headers=headers)
            if response.status_code == 201:
                flash('Account created successfully! You can log in now.', 'success')
                user_data = response.json()
                user = User(user_data["id"], user_data["name"],
                    user_data["email"], form.password.data
                )
                users.append(user)
                return redirect(url_for('login'))
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

"""@app.route('/login', methods=['GET', 'POST'])
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

    return render_template('login.html', form=form, error=error)"""

@app.route('/login', methods=['GET', 'POST'])
def login():
    if current_user.is_authenticated:
        return redirect(url_for('index'))
    
    error = None
    form = LoginForm(request.form if request.method == 'POST' else None)

    if request.method == "POST" and form.validate():
        # Preparar la solicitud al backend para validar el login
        payload = {
            'email': form.email.data,
            'password': form.password.data
        }

        try:
            headers = {
                'Content-Type': 'application/json',
            }
            # Hacer una solicitud POST al endpoint checkLogin del backend
            response = requests.post("http://backend-rest:8080/Service/checkLogin", json=payload, headers=headers)

            if response.status_code == 200:  # Login exitoso
                user_data = response.json()  # Recibir datos del usuario en formato JSON

                # Crear un objeto usuario aquí (esto depende de tu implementación en Flask-Login)
                user = User(user_data["id"], user_data["name"],
                    user_data["email"], form.password.data
                )
                users.append(user)

                # Loguear al usuario
                login_user(user, remember=form.remember_me.data)
                return redirect(url_for('index'))

            elif response.status_code == 403:  # Si el login falla
                error = 'Invalid Credentials. Please try again.'
            else:
                error = 'Something went wrong. Please try again later.'
        
        except requests.exceptions.RequestException as e:
            error = f"Error: {e}"

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
        if str(user.id) == user_id:
            return user
    return None

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=int(os.environ.get('PORT', 5010)))
