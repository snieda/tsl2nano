<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>TSL2 Applications</title>
    <style>
        body {
            background: radial-gradient(#9999FF, #000000);
            text-align: center;
            color: white;
        }

        h1 {
            position: relative;
            font-size: 8em;
            transition: 0.5s;
            font-family: Arial, Helvetica, sans-serif;
            text-shadow: 0 1px 0 #ccc, 0 2px 0 #ccc,
                0 3px 0 #ccc, 0 4px 0 #ccc,
                0 5px 0 #ccc, 0 6px 0 #ccc,
                0 7px 0 #ccc, 0 8px 0 #ccc,
                0 9px 0 #ccc, 0 10px 0 #ccc,
                0 11px 0 #ccc, 0 12px 0 #ccc,
                0 20px 30px rgba(0, 0, 0, 0.5);
        }

        a {
            color: lightskyblue;
        }

        div {
            font-size: 1.5em;
        }

    </style>
</head>

<body>
    <h1>TSL2 Nano H5 Framework</h1>
    <h2>Persistence: ${bean}</h2>
    <div>Version: ${app.update.current.version}</div>
    <div><a href=${service.url}>Service Url</a></div>
    <div><a href=${app.ssl.wss.protocol}>WebSocket Protocol</a></div>
    <div><a href=https://gitlab.com/snieda/tsl2nano/-/blob/master/tsl2.nano.h5/nano.h5.md.html?ref_type=heads>Current internal Documentation</a></div>
    <div>Code Sources:</div>
    <div><a href=https://sourceforge.net/projects/tsl2nano/>Sourceforge</a></div>
    <div><a href=https://github.com/snieda/tsl2nano>GitHub</a></div>
    <div><a href=https://gitlab.com/snieda/tsl2nano>GitLab</a></div>
</body>

</html>