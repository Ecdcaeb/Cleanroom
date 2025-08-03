/*
 * Minecraft Forge
 * Copyright (c) 2016-2020.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.fml.common.eventhandler;



public interface IEventListener
{
    void invoke(Event event);

    public static class EventListenerContext {
        public ListenerList bus;
        public IEventListener listener;
        public EventListenerContext(ListenerList bus, IEventListener listener) {
            this.bus = bus;
            this.listener = listener;
        }
        public EventListenerContext pass(IEventListener listener) {
            this.listener = listener;
        }
        public static EventListenerContext create(IEventListener listener, ListenerList bus) {
            return new EventListenerContext(bus, listener);
        }
    }
}
