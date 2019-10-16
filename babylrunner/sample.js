// Sample Babylscript program that creates a French function called "afficher"
// for displaying text to console out. It uses Rhino's ability to call into
// Java to do the actual displaying of text.
//
// This "afficher" function is translated as "show" in English, and in English
// mode, the "show" function is used to display 'Hello world" to the screen 

---fr---
fonction afficher(texte) {
    java.lang.System.out.println(texte);
}
ceci['en':'show'] = ceci['ro':'afișează'] = 'afficher';

---en---
show('Hello world');