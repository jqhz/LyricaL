from py4j.clientserver import ClientServer, JavaParameters, PythonParameters
import syncedlyrics

class Synced_Lyrics(object):
    def lyrics_search(self,track_name,artist):
        lyrics = syncedlyrics.search(track_name + " " + artist,enhanced=True)
        if lyrics:
            return lyrics
        else:
            return "No Lyrics Found"
lyrics_fetcher = Synced_Lyrics()
gateway = ClientServer(
    java_parameters=JavaParameters(port=0),
    python_parameters=PythonParameters(port=0),
    python_server_entry_point=lyrics_fetcher
)