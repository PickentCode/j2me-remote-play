# j2me-remote-play

I discovered that one old feature phone in my collection (Sony Ericsson Elm) has Wi-Fi and that led me to write this. It's very simple: the server runs on my PC, grabs screenshots, encodes them as heavily compressed JPEGs, splits them into smaller chunks and sends them to the phone. The phone decodes each image, draws it onto the canvas and sends the key states back, which the server then emulates using Java's Robot class.

No sound (I just used a Bluetooth speaker connected to the PC). It's written for my Sony Ericsson Elm (screen size, max chunk size and key codes are all set for the Elm), so it needs changes for other phones. The same goes for the PC: the IP address and screen resolution are set for my PC. It also doesn't handle lost packets, but during my testing that didn't happen once.
