window.onload = function() {

  // init session
  const loading_img = document.getElementById('loading-img');
  loading_img.style.opacity = 0;
  sessionStorage.setItem("session", sessionStorage.getItem('session') || Math.floor(100000* Math.random()));

  const fetchUrl = function(path, option) {
    loading_img.style.opacity = 1;
    return fetch(path + '?session=' + sessionStorage.getItem('session'), option)
      .then(res => {
        loading_img.style.opacity = 0;
        if (res.status === 200)
          return res;
        return res.text().then(message => {
          throw new Error(message || ('Requête échouée (' + res.status + ').'));
        });
      })
  }

  const my_canvas = document.getElementById('mon_canvas');
  const canvas = new Canvas(my_canvas,8);
  const zoom_value = document.getElementById('zoom-value');
  const updateZoomValue = function() {
    zoom_value.textContent = 'Zoom: ' + Math.round(canvas.rayon / 8 * 100) + '%';
  }
  canvas.setWidth(window.innerWidth);
  canvas.setHeight(window.innerHeight-50);
  updateZoomValue();
  const plateau = new Plateau();
  let refreshing = null;
  let playing = false;

  const refreshPlateau = function() {
    if (refreshing)
      return refreshing

    refreshing = fetchUrl('/grid').then(res => {
      return res.json().then(json => {
        plateau.data = json;      
        plateau.rebuildIndex();
        canvas.draw(plateau);
        refreshing = null;
      })
    })
    return refreshing
  }

  // empty input
  const empty_input = document.getElementById('empty-input');
  empty_input.addEventListener('click', () => {
    fetchUrl('/grid/empty', {method: 'post'})
      .then(() => refreshPlateau())
  });
  
  // refresh input
  const refresh_input = document.getElementById('refresh-input');
  refresh_input.addEventListener('click', refreshPlateau);
  refreshPlateau()

  // next input
  const next_input = document.getElementById('next-input');
  const nextPlateau = function() {
    return fetchUrl('/grid/next', {method: 'post'})
      .then(() => refreshPlateau())
  }
  
  next_input.addEventListener('click', nextPlateau, false);

  // play input
  const play_input = document.getElementById('play-input');
  const speed_input = document.getElementById('speed-input');
  const start_play = function() {
    if (playing) {
      const start = new Date();
      nextPlateau().then(() => {
        const end = new Date();
        const waiting_time = Math.max((10000 / parseInt(speed_input.value))  - (end - start), 0);
        setTimeout(start_play, waiting_time);
      })
    }
  }
  
  play_input.addEventListener('click', () => {
    playing = !playing;
    next_input.disabled = playing;
    play_input.value = (playing) ? 'stop' : 'lecture';
    start_play();
  })

  // add
  const addOrRemove = function(x,y) {
    return fetchUrl('/grid/change', {'method': 'put', 'body': JSON.stringify({'x':x, 'y':y})})
  }

  const ajout_cellule = function(e){
    if(!canvas.wasdrag){
        var x = Math.floor((e.clientX-my_canvas.getBoundingClientRect().left + canvas.origine[0])/(2*canvas.rayon)),
	    y = Math.floor((e.clientY-my_canvas.getBoundingClientRect().top + canvas.origine[1])/(2*canvas.rayon));
      addOrRemove(x,y).then(refreshPlateau)
    }
    canvas.wasdrag = (canvas.wasdrag)? false : false;
}
    
  my_canvas.addEventListener('click',ajout_cellule,false);

  // save
  const save_input = document.getElementById('save-input')

  save_input.addEventListener('click', () => {
    fetchUrl('/grid/save', {method: 'post'})
      .then(refreshPlateau)
  })

  // cancel
  const cancel_input = document.getElementById('cancel-input')

  cancel_input.addEventListener('click', () => {
    fetchUrl('/grid/cancel', {method: 'post'})
      .then(refreshPlateau)
  })

  // import
  const rle_input = document.getElementById('rle-input');
  const import_input = document.getElementById('import-input');
  const import_status = document.getElementById('import-status');
  const importRLE = function(url) {
    import_input.disabled = true;
    import_status.className = '';
    import_status.textContent = 'Import en cours...';
    return fetchUrl('/grid/rle', {method: 'put', body: url})
      .then(() => refreshPlateau())
      .then(() => {
        canvas.focusOn(plateau);
        updateZoomValue();
        canvas.draw(plateau);
        import_status.className = 'success';
        import_status.textContent = 'Import terminé (' + plateau.data.length + ' cellules).';
      })
      .catch(error => {
        import_status.className = 'error';
        import_status.textContent = error.message || "Echec de l'import.";
      })
      .finally(() => {
        import_input.disabled = false;
      });
  }

  import_input.addEventListener('click', () => {
    if (!rle_input.value.trim()) {
      import_status.className = 'error';
      import_status.textContent = 'Entrez une URL RLE.';
      return;
    }
    importRLE(rle_input.value.trim());
  }, false);
  
  // zoom et dezoom
  my_canvas.addEventListener('mousewheel',mouseWheel,false);
  my_canvas.addEventListener('DOMMouseScroll',mouseWheel,false);

  // zoom with keyboard (+ / - or arrows)
  document.addEventListener('keydown', function(e) {
    // Determine zoom direction
    let delta = 0;
    if (e.key === '+' || e.key === '=' || e.key === 'ArrowUp') delta = 1;
    if (e.key === '-' || e.key === '_' || e.key === 'ArrowDown') delta = -1;

    if (delta !== 0) {
      // zoom centered on canvas center
      const posx = my_canvas.width / 2;
      const posy = my_canvas.height / 2;
      canvas.zoom(delta, posx, posy);
      updateZoomValue();
      canvas.draw(plateau);
    }
  });


  function mouseWheel(e) {
    // sens du scroll
    var delta = Math.max(-1, Math.min(1, (e.wheelDelta || -e.detail)));
    var posx = e.clientX-my_canvas.getBoundingClientRect().left;
    var posy = e.clientY-my_canvas.getBoundingClientRect().top;
    canvas.zoom(delta,posx,posy);
    updateZoomValue();
    canvas.draw(plateau);
  }

  //deplacement 
  let decompte;
  my_canvas.addEventListener('mouseout',function(){
    //si on sort la souris du canvas pendant un déplacement on l'arrete
    canvas.drag = false;
  },false);
  my_canvas.addEventListener('mousedown',function(e){
    //on attend 10ms avant de lancer le déplacement
    decompte = setTimeout(function() {canvas.drag = [e.clientX,e.clientY];},100); 
  },false);

  my_canvas.addEventListener('mouseup',function(){
    //si le décompte est encore en route alors qu'on lance la souris on le coupe
    clearTimeout(decompte);
    canvas.drag = false;
    canvas.draw(plateau);
  }, false);

  my_canvas.addEventListener('mousemove', function(e){
    if(canvas.drag){
      if(canvas.wasdrag){
	//signifie que ce n'est pas le premier mouvement ie canvas.drag est un tableau
	canvas.origine[0] -= e.clientX - canvas.drag[0];
	canvas.origine[1] -= e.clientY - canvas.drag[1];
	canvas.wasdrag++;
	//on trace le plateau 1 fois sur 10  
	if(!(canvas.wasdrag%10)){canvas.draw(plateau);}
      } else {
	//on previent que le prochain evenement click et du au deplacement
	canvas.wasdrag = 1;
      }
      canvas.drag = [e.clientX,e.clientY];
    }
  },true);

  window.addEventListener('resize',function(){
    canvas.setWidth(document.getElementById('canvas').offsetWidth);
    canvas.setHeight(document.getElementById('canvas').offsetHeight)
    canvas.draw(plateau);
  },false);

}

function Plateau() {
  
  this.data = [];
  this.nombre = 0;
  this.generation = 0;
  this.bucketSize = 64;
  this.buckets = new Map();

  this.rebuildIndex = function() {
    this.buckets.clear();
    for (let i = 0; i < this.data.length; i++) {
      const cell = this.data[i];
      const bucketX = Math.floor(cell.x / this.bucketSize);
      const bucketY = Math.floor(cell.y / this.bucketSize);
      const key = bucketX + ',' + bucketY;
      let bucket = this.buckets.get(key);
      if (!bucket) {
        bucket = [];
        this.buckets.set(key, bucket);
      }
      bucket.push(cell);
    }
  }

  this.empty = function() {
    this.data = [];
    this.buckets.clear();
    this.nombre = 0;
    this.generation = 0;
  }
}

function Canvas(canvas, r){
  this.canvas = canvas;
  this.context = this.canvas.getContext('2d');
  this.rayon = r;
  this.drag = false;
  this.wasdrag = false;
  this.mode = false; // signifie que le jeu n'est pas en marche
  this.origine = [-1,-1]; // position du cote haut gauche de l'affichage variable suivant le rayon sur le plan infini
  this.minRayon = 0.01
  this.zoomStep = 1.08
  
  this.zoom = function(d,x,y) { 
    //zoom ou dezoom en laissant x,y à la meme position
    const nextRayon = Math.max(this.minRayon, d > 0 ? this.rayon * this.zoomStep : this.rayon / this.zoomStep);
    if (nextRayon === this.rayon) return;
    this.origine[0] = (nextRayon / this.rayon) * (this.origine[0] + x) - x;
    this.origine[1] = (nextRayon / this.rayon) * (this.origine[1] + y) - y;
    this.rayon = nextRayon;
  }

  this.getWidth = function() {
    return this.canvas.width;
  }
  this.setWidth = function(w) {
    this.canvas.width = w;
  }
  this.getHeight = function() {
    return this.canvas.height;
  }
  this.setHeight = function(h) {
    this.canvas.height = h;
  }

  this.focusOn = function(p) {
    if (!p.data.length) return;

    let minX = p.data[0].x;
    let maxX = p.data[0].x;
    let minY = p.data[0].y;
    let maxY = p.data[0].y;
    for (let i = 1; i < p.data.length; i++) {
      minX = Math.min(minX, p.data[i].x);
      maxX = Math.max(maxX, p.data[i].x);
      minY = Math.min(minY, p.data[i].y);
      maxY = Math.max(maxY, p.data[i].y);
    }

    const padding = 2;
    const width = maxX - minX + 1 + padding * 2;
    const height = maxY - minY + 1 + padding * 2;
    const fitRayon = Math.min(this.getWidth() / (2 * width), this.getHeight() / (2 * height));
    if (fitRayon < this.rayon) this.rayon = Math.max(this.minRayon, fitRayon);

    this.origine[0] = ((minX + maxX + 1) * this.rayon) - this.getWidth() / 2;
    this.origine[1] = ((minY + maxY + 1) * this.rayon) - this.getHeight() / 2;
  }

  this.draw = function(p) {
    // on nettoie le canevas
    this.context.clearRect(0,0,this.getWidth(),this.getHeight()); 

    const minX = Math.floor(this.origine[0] / (2 * this.rayon));
    const maxX = Math.ceil((this.origine[0] + this.getWidth()) / (2 * this.rayon));
    const minY = Math.floor(this.origine[1] / (2 * this.rayon));
    const maxY = Math.ceil((this.origine[1] + this.getHeight()) / (2 * this.rayon));
    const minBucketX = Math.floor(minX / p.bucketSize);
    const maxBucketX = Math.floor(maxX / p.bucketSize);
    const minBucketY = Math.floor(minY / p.bucketSize);
    const maxBucketY = Math.floor(maxY / p.bucketSize);

    this.context.fillStyle = "#E1170D";
    for (let bucketX = minBucketX; bucketX <= maxBucketX; bucketX++) {
      for (let bucketY = minBucketY; bucketY <= maxBucketY; bucketY++) {
        const bucket = p.buckets.get(bucketX + ',' + bucketY);
        if (!bucket) continue;
        for (let i = 0; i < bucket.length; i++) {
          const cell = bucket[i];
          if (cell.x < minX || cell.x > maxX || cell.y < minY || cell.y > maxY) continue;
          this.context.beginPath();
          this.context.arc(this.rayon*(2*cell.x+1) - this.origine[0],
                           this.rayon*(2*cell.y+1) - this.origine[1],
                           this.rayon,0,2*Math.PI);
          this.context.fill();
          this.context.closePath();
        }
      }
    }
    
    if(!this.mode && this.rayon > 5){
      //on cherche les premieres coord 
      this.context.strokeStyle ='#A2967D';
      this.context.fillStyle ='#A2967D';
      this.context.font = '8px sans-serif';
      var x =  - (this.origine[0] %(2*this.rayon)),
	  y =  - (this.origine[1] %(2*this.rayon)),
          cx = Math.floor(this.origine[0] / (2*this.rayon)),
          cy = Math.floor(this.origine[1] / (2*this.rayon));

      cx = (x > 0) ? cx +1 : cx;
      cy = (y > 0) ? cy +1 : cy;
      
      while(x < this.getWidth())
      {
        if (x > 2 * this.rayon) {
 	  this.context.beginPath();
	  this.context.moveTo(x, 2 * this.rayon);
	  this.context.lineTo(x, this.getHeight());
	  this.context.stroke();
	  this.context.closePath();
        }
        this.context.textAlign = 'center';
        this.context.fillText(cx, x + this.rayon, 2*this.rayon - 2);
	x+= 2*this.rayon;
        cx++;
      }
      while(y < this.getHeight())
      {
        if (y > 2 * this.rayon) {
 	  this.context.beginPath();
	  this.context.moveTo(2 * this.rayon, y);
	  this.context.lineTo(this.getWidth(),y);
	  this.context.stroke();
	  this.context.closePath();
        }
        this.context.textAlign = 'right';
        this.context.fillText(cy, 2*this.rayon - 2, y + 1.5 * this.rayon);
	y+= 2*this.rayon;
        cy++;
      }
      
    }
  }
}
