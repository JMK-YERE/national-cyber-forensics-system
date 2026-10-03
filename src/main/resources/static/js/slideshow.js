document.addEventListener('DOMContentLoaded',function(){
 const slides=[...document.querySelectorAll('.hero-slide')];
 const dots=[...document.querySelectorAll('.slider-dots button')];
 if(!slides.length)return;
 let current=0,timer;
 function go(n){slides[current].classList.remove('active');if(dots[current])dots[current].classList.remove('active');current=(n+slides.length)%slides.length;slides[current].classList.add('active');if(dots[current])dots[current].classList.add('active')}
 function restart(){clearInterval(timer);timer=setInterval(()=>go(current+1),6500)}
 dots.forEach((d,i)=>d.addEventListener('click',()=>{go(i);restart()}));
 restart();
});