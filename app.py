from flask import Flask, request, session, redirect, url_for, render_template, flash

app = Flask(__name__)

# Load default config and override config from an environment variable
app.config.update(dict(SECRET_KEY='development key',))

if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0')

@app.before_first_request
def before_first_request():
    session.clear()

@app.route('/', methods=['GET', 'POST'])
@app.route('/home', methods=['GET', 'POST'])
@app.route('/homepage', methods=['GET', 'POST'])
def home():
    if request.method == 'GET':
        return render_template('homepage.html')
    elif request.form.getlist('files'):
        session['files'] = request.form.getlist('files')
        session['engine_loaded'] = True
        return redirect(url_for('engine'))
    else:
        flash("Something went wrong, please try again.")
        return render_template('homepage.html')

@app.route('/engine')
def engine():
    if not engine_loaded():
        flash("Please initialize the engine")
        return redirect(url_for('home'))
    return render_template('engine.html')

@app.route('/search', methods=['GET', 'POST'])
def search():
    if not engine_loaded():
        flash("Please initialize the engine")
        return redirect(url_for('home'))
    elif request.method == 'GET':
        return render_template('search.html')
    elif request.form['term']:
        term = request.form['term']
        data = [[1,'histories','1kinghenryiv',169],
                [2,'histories','1kinghenryiv',160],
                [3,'histories','2kinghenryiv',179],
                [4,'histories','2kinghenryiv',340]]
        return render_template('search-term.html', term=term, time=999, data=data)

    else:
        flash("Something went wrong, please try again.")
        return render_template('search.html')

@app.route('/top-n', methods=['GET', 'POST'])
def top_n():
    if not engine_loaded():
        flash("Please initialize the engine")
        return redirect(url_for('home'))
    elif request.method == 'GET':
        return render_template('top-n.html')
    elif request.form['top-n']:
        data = [['KING',5000], ['HENRY',4500], ['THE',4000], ['FOURTH',3500], ['SIR',3000], ['WALTER',2500], ['BLUNT',2000], ['OWEN',1500], ['GELNDOWER',1000], ['RICHARD',500]]
        return render_template('top-n-results.html', data = data)
    else:
        flash("Something went wrong, please try again.")
        return render_template('top-n.html')

def engine_loaded():
    if 'engine_loaded' not in session or not session['engine_loaded']:
        return False
    return True