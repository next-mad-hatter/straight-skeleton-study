package at.tugraz.igi.events;

import java.util.Comparator;

public class EventComparator implements Comparator<Event> {

	@Override
	public int compare(Event ev1, Event ev2) {
		if (ev1 != null && ev2 != null) {
			if (ev1.getCollapsingTime() < ev2.getCollapsingTime())
				return -1;
			else if (ev1.getCollapsingTime() > ev2.getCollapsingTime()) {
				return 1;
			}
		}
		return 0;
	}

}
