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
        else
          return Promise.reject();
      })
  }

  const my_canvas = document.getElementById('mon_canvas');
  const canvas = new Canvas(my_canvas,8);
  canvas.setWidth(window.innerWidth);
  canvas.setHeight(window.innerHeight-50);
  const plateau = new Plateau();
  let refreshing = null;
  let playing = false;

  const refreshPlateau = function() {
    if (refreshing)
      return refreshing

    refreshing = fetchUrl('/grid').then(res => {
      res.json().then(json => {
        plateau.data = json;      
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
  const importRLE = function(url) {
    return fetchUrl('/grid/rle', {method: 'put', body: url})
      .then(() => refreshPlateau())
  }

  import_input.addEventListener('click', () => {
    importRLE(rle_input.value);
  }, false);
  
  // zoom et dezoom
  my_canvas.addEventListener('mousewheel',mouseWheel,false);
  my_canvas.addEventListener('DOMMouseScroll',mouseWheel,false);

  function mouseWheel(e) {
    // sens du scroll
    var delta = Math.max(-1, Math.min(1, (e.wheelDelta || -e.detail)));
    var posx = e.clientX-my_canvas.getBoundingClientRect().left;
    var posy = e.clientY-my_canvas.getBoundingClientRect().top;
    canvas.zoom(delta,posx,posy);
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

  this.empty = function() {
    this.data = [];
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
  this.minRayon = 0.5
  
  this.zoom = function(d,x,y) { 
    //zoom ou dezoom en laissant x,y à la meme position
    if((this.rayon<10 && this.rayon > this.minRayon) ||
       (this.rayon >= 10 && d<0) ||
       (this.rayon<= this.minRayon && d>0)){
      this.origine[0] = ((this.rayon + d/2)/this.rayon)*(this.origine[0] + x) - x;
      this.origine[1] = ((this.rayon + d/2 ) /this.rayon)*(this.origine[1] + y) - y;
      if(d > 0) { this.rayon += 0.5 }
      else {this.rayon -= 0.5 }
    }
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

  this.draw = function(p) {
    // on nettoie le canevas
    this.context.clearRect(0,0,this.getWidth(),this.getHeight()); 

    for(i=0;i < p.data.length; i++) {

      if(2*this.rayon*(p.data[i].x+1) > this.origine[0]  && 
         2*this.rayon*p.data[i].x < this.origine[0] + this.getWidth() &&
         2*this.rayon*(p.data[i].y+1) > this.origine[1] && 
         2*this.rayon*p.data[i].y < this.origine[1] + this.getHeight() ) {

	this.context.beginPath();
	this.context.arc(this.rayon*(2*p.data[i].x+1) - this.origine[0],
			 this.rayon*(2*p.data[i].y+1) - this.origine[1],
			 this.rayon,0,2*Math.PI);
	this.context.fillStyle = "#E1170D";
	this.context.fill();
	this.context.closePath();
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
