<#ftl encoding="utf-8">

    <head>
        <title>Jeu de la vie</title>
        <link rel="stylesheet" href="style.css"/>
        <script src="life.js"></script>
        <meta charset="utf-8" />
    </head>

    <body xmlns="http://www.w3.org/1999/html">
        <div>
            <img id="loading-img" src="./loading.svg" />
            <input id="empty-input" type="button" value="vider" />
            <input id="refresh-input" type="button" value="rafraichir"/>
            <div class="box">
                <input id="cancel-input" type="button" value="annuler" />
                <input id="save-input" type="button" value="sauvegarder" />
            </div>
            <div class="box">
                <label for="speed-input">Vitesse: </label>
                <input id="speed-input" type="range" value="1" min="1" max="50" />
                <input id="play-input" type="button" value="lecture" />
                <input id="next-input" type="button" value="suivant"/>
            </div>
            <div class="box">
                <label for="rle-input">RLE: </label>
                <input id="rle-input" type="url" placeholder="url" />
                <input id="import-input" type="button" value="importer" />
            </div>
        </div>

        <div id="canvas">
            <canvas id="mon_canvas" >
                Message pour les navigateurs ne supportant pas encore canvas.
            </canvas>
        </div>

    </body>

</html>
