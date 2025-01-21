from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
import syncedlyrics
import socket

class Synced_Lyrics(object):
    def lyrics_search(self,track_name,artist):
        lyrics = syncedlyrics.search(track_name + " " + artist,enhanced=True)
        if lyrics:
            return lyrics
        else:
            return "No Lyrics Found"
        
def get_available_port(start_port=49152, end_port=65535):
    for port in range(start_port, end_port + 1):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            result = s.connect_ex(('127.0.0.1', port))
            if result != 0:  # Port is available
                return port
    return None  # No available port found
java_port = get_available_port()
python_port=0
lyrics_fetcher = Synced_Lyrics()
gateway = ClientServer(
    java_parameters=JavaParameters(port=java_port),
    python_parameters=PythonParameters(port=python_port),
    python_server_entry_point=lyrics_fetcher
)
java_port = gateway.java_parameters.address[1]
python_port = gateway.python_parameters.address[1]
'''try:
    gateway.start()
except KeyboardInterrupt:
    print("SHUTDOWN")
finally:
    gateway.shutdown()'''