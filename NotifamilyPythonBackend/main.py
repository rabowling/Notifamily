import firebase_admin
from firebase_admin import credentials, db, messaging
import time
import datetime
import math

notification_list = []
user_list = []
timezone = datetime.timezone(-datetime.timedelta(hours=8))

min_distance = 200 # distance in meters required to trigger a notification

def compare_time(time_dict):
	date_obj = datetime.datetime(time_dict["year"],
	                             time_dict["month"]+1,
	                             time_dict["day"],
	                             time_dict["hour"],
	                             time_dict["minute"],
	                             tzinfo=timezone)
	now = datetime.datetime.now(timezone)
	print(date_obj)
	print(now)
	print(date_obj <= now)
	return date_obj <= now

def notify_user(notification, notif_id, completed):
	try:
		message = make_notif(notification, completed)
		messaging.send(message)
		if completed:
			db.reference().child("notifications").child(notif_id).update({"parentNotified": True})
		else:
			db.reference().child("notifications").child(notif_id).update({"notified": True})
	except messaging.ApiCallError as e:
		print(e.detail.response.json())

def make_notif(notification, completed):
	notif_obj = messaging.Notification(title="Notifamily Alert",
	                                   body=user_list[notification["user"]]["name"] + " completed a task!" if completed else notification["content"])
	deviceToken = user_list[user_list[notification["user"]]["parentId"]]["deviceToken"] if completed else user_list[notification["user"]]["deviceToken"]
	data = {"NOTIF_ID": notification["id"]}
	message = messaging.Message(token=deviceToken,
	                            notification=notif_obj,
	                            data=data)
	return message

def update_notif_list(event):
	global notification_list
	notification_list = db.reference().child("notifications").get()

def update_user_list(event):
	global user_list
	user_list = db.reference().child("users").get()

def haversine(latA, lngA, latB, lngB):
	# Formula and pseudocode from https://www.movable-type.co.uk/scripts/latlong.html
	r = 6371000
	phi_1 = math.radians(latA)
	phi_2 = math.radians(latB)
	delta_phi = math.radians(latB - latA)
	delta_lambda = math.radians(lngB - lngA)

	a = math.pow(math.sin(delta_phi / 2), 2) + math.cos(phi_1) * math.cos(phi_2) * math.pow(math.sin(delta_lambda / 2), 2)
	c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))

	return r * c

def check_location(info):
	if "location" in info:
		latitude = info["location"]["latitude"]
		longitude = info["location"]["longitude"]
		user = user_list[info["user"]]
		if "lastLocation" in user:
			user_lat = user["lastLocation"]["latitude"]
			user_lng = user["lastLocation"]["longitude"]
			distance = haversine(latitude, longitude, user_lat, user_lng)
			if distance <= min_distance:
				return True
			else:
				return False
		else:
			return False
	else:
		return True

cred = credentials.Certificate("firebaseServiceAccountKey.json")
firebase_admin.initialize_app(cred, {'databaseURL': 'https://notifamily-d7bdb.firebaseio.com/'})
ref = db.reference()
listener1 = ref.child("notifications").listen(update_notif_list)
listener2 = ref.child("users").listen(update_user_list)
while True:
	time.sleep(5)
	for notification in notification_list:
		info = notification_list[notification]
		if not info["notified"]:
			if check_location(info) and compare_time(info["time"]):
				notify_user(info, notification, False)
				print("Attempted to notifiy!")
		else:
			if not info["parentNotified"] and info["completed"]:
				notify_user(info, notification, True)

listener1.close()
listener2.close()