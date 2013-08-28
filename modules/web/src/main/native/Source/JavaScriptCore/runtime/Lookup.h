/*
 *  Copyright (C) 1999-2000 Harri Porten (porten@kde.org)
 *  Copyright (C) 2003, 2006, 2007, 2008, 2009 Apple Inc. All rights reserved.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */

#ifndef Lookup_h
#define Lookup_h

#include "CallFrame.h"
#include "Intrinsic.h"
#include "Identifier.h"
#include "JSGlobalObject.h"
#include "PropertySlot.h"
#include <stdio.h>
#include <wtf/Assertions.h>

namespace JSC {
    // Hash table generated by the create_hash_table script.
    struct HashTableValue {
        const char* key; // property name
        unsigned char attributes; // JSObject attributes
        intptr_t value1;
        intptr_t value2;
        Intrinsic intrinsic;
    };

    // FIXME: There is no reason this get function can't be simpler.
    // ie. typedef JSValue (*GetFunction)(ExecState*, JSObject* baseObject)
    typedef PropertySlot::GetValueFunc GetFunction;
    typedef void (*PutFunction)(ExecState*, JSObject* baseObject, JSValue value);

    class HashEntry {
        WTF_MAKE_FAST_ALLOCATED;
    public:
        void initialize(StringImpl* key, unsigned char attributes, intptr_t v1, intptr_t v2, Intrinsic intrinsic)
        {
            m_key = key;
            m_attributes = attributes;
            m_u.store.value1 = v1;
            m_u.store.value2 = v2;
            m_intrinsic = intrinsic;
            m_next = 0;
        }

        void setKey(StringImpl* key) { m_key = key; }
        StringImpl* key() const { return m_key; }

        unsigned char attributes() const { return m_attributes; }

        Intrinsic intrinsic() const
        {
            ASSERT(m_attributes & Function);
            return m_intrinsic;
        }

        NativeFunction function() const { ASSERT(m_attributes & Function); return m_u.function.functionValue; }
        unsigned char functionLength() const { ASSERT(m_attributes & Function); return static_cast<unsigned char>(m_u.function.length); }

        GetFunction propertyGetter() const { ASSERT(!(m_attributes & Function)); return m_u.property.get; }
        PutFunction propertyPutter() const { ASSERT(!(m_attributes & Function)); return m_u.property.put; }

        intptr_t lexerValue() const { ASSERT(!m_attributes); return m_u.lexer.value; }

        void setNext(HashEntry *next) { m_next = next; }
        HashEntry* next() const { return m_next; }

    private:
        StringImpl* m_key;
        unsigned char m_attributes; // JSObject attributes
        Intrinsic m_intrinsic;

        union {
            struct {
                intptr_t value1;
                intptr_t value2;
            } store;
            struct {
                NativeFunction functionValue;
                intptr_t length; // number of arguments for function
            } function;
            struct {
                GetFunction get;
                PutFunction put;
            } property;
            struct {
                intptr_t value;
                intptr_t unused;
            } lexer;
        } m_u;

        HashEntry* m_next;
    };

    struct HashTable {

        int compactSize;
        int compactHashSizeMask;

        const HashTableValue* values; // Fixed values generated by script.
        mutable const HashEntry* table; // Table allocated at runtime.

        ALWAYS_INLINE HashTable copy() const
        {
            // Don't copy dynamic table since it's thread specific.
            HashTable result = { compactSize, compactHashSizeMask, values, 0 };
            return result;
        }

        ALWAYS_INLINE void initializeIfNeeded(VM* vm) const
        {
            if (!table)
                createTable(vm);
        }

        ALWAYS_INLINE void initializeIfNeeded(ExecState* exec) const
        {
            if (!table)
                createTable(&exec->vm());
        }

        JS_EXPORT_PRIVATE void deleteTable() const;

        // Find an entry in the table, and return the entry.
        ALWAYS_INLINE const HashEntry* entry(VM* vm, PropertyName identifier) const
        {
            initializeIfNeeded(vm);
            return entry(identifier);
        }

        ALWAYS_INLINE const HashEntry* entry(ExecState* exec, PropertyName identifier) const
        {
            initializeIfNeeded(exec);
            return entry(identifier);
        }

        class ConstIterator {
        public:
            ConstIterator(const HashTable* table, int position)
                : m_table(table)
                , m_position(position)
            {
                skipInvalidKeys();
            }

            const HashEntry* operator->()
            {
                return &m_table->table[m_position];
            }

            const HashEntry* operator*()
            {
                return &m_table->table[m_position];
            }

            bool operator!=(const ConstIterator& other)
            {
                ASSERT(m_table == other.m_table);
                return m_position != other.m_position;
            }
            
            ConstIterator& operator++()
            {
                ASSERT(m_position < m_table->compactSize);
                ++m_position;
                skipInvalidKeys();
                return *this;
            }

        private:
            void skipInvalidKeys()
            {
                ASSERT(m_position <= m_table->compactSize);
                while (m_position < m_table->compactSize && !m_table->table[m_position].key())
                    ++m_position;
                ASSERT(m_position <= m_table->compactSize);
            }
            
            const HashTable* m_table;
            int m_position;
        };

        ConstIterator begin(VM& vm) const
        {
            initializeIfNeeded(&vm);
            return ConstIterator(this, 0);
        }
        ConstIterator end(VM& vm) const
        {
            initializeIfNeeded(&vm);
            return ConstIterator(this, compactSize);
        }

    private:
        ALWAYS_INLINE const HashEntry* entry(PropertyName propertyName) const
        {
            StringImpl* impl = propertyName.publicName();
            if (!impl)
                return 0;
        
            ASSERT(table);

            const HashEntry* entry = &table[impl->existingHash() & compactHashSizeMask];

            if (!entry->key())
                return 0;

            do {
                if (entry->key() == impl)
                    return entry;
                entry = entry->next();
            } while (entry);

            return 0;
        }

        // Convert the hash table keys to identifiers.
        JS_EXPORT_PRIVATE void createTable(VM*) const;
    };

    JS_EXPORT_PRIVATE bool setUpStaticFunctionSlot(ExecState*, const HashEntry*, JSObject* thisObject, PropertyName, PropertySlot&);

    /**
     * This method does it all (looking in the hashtable, checking for function
     * overrides, creating the function or retrieving from cache, calling
     * getValueProperty in case of a non-function property, forwarding to parent if
     * unknown property).
     */
    template <class ThisImp, class ParentImp>
    inline bool getStaticPropertySlot(ExecState* exec, const HashTable* table, ThisImp* thisObj, PropertyName propertyName, PropertySlot& slot)
    {
        const HashEntry* entry = table->entry(exec, propertyName);

        if (!entry) // not found, forward to parent
            return ParentImp::getOwnPropertySlot(thisObj, exec, propertyName, slot);

        if (entry->attributes() & Function)
            return setUpStaticFunctionSlot(exec, entry, thisObj, propertyName, slot);

        slot.setCacheableCustom(thisObj, entry->propertyGetter());
        return true;
    }

    template <class ThisImp, class ParentImp>
    inline bool getStaticPropertyDescriptor(ExecState* exec, const HashTable* table, ThisImp* thisObj, PropertyName propertyName, PropertyDescriptor& descriptor)
    {
        const HashEntry* entry = table->entry(exec, propertyName);
        
        if (!entry) // not found, forward to parent
            return ParentImp::getOwnPropertyDescriptor(thisObj, exec, propertyName, descriptor);
 
        PropertySlot slot;
        if (entry->attributes() & Function) {
            bool present = setUpStaticFunctionSlot(exec, entry, thisObj, propertyName, slot);
            if (present)
                descriptor.setDescriptor(slot.getValue(exec, propertyName), entry->attributes());
            return present;
        }

        slot.setCustom(thisObj, entry->propertyGetter());
        descriptor.setDescriptor(slot.getValue(exec, propertyName), entry->attributes());
        return true;
    }

    /**
     * Simplified version of getStaticPropertySlot in case there are only functions.
     * Using this instead of getStaticPropertySlot allows 'this' to avoid implementing
     * a dummy getValueProperty.
     */
    template <class ParentImp>
    inline bool getStaticFunctionSlot(ExecState* exec, const HashTable* table, JSObject* thisObj, PropertyName propertyName, PropertySlot& slot)
    {
        if (ParentImp::getOwnPropertySlot(thisObj, exec, propertyName, slot))
            return true;

        const HashEntry* entry = table->entry(exec, propertyName);
        if (!entry)
            return false;

        return setUpStaticFunctionSlot(exec, entry, thisObj, propertyName, slot);
    }
    
    /**
     * Simplified version of getStaticPropertyDescriptor in case there are only functions.
     * Using this instead of getStaticPropertyDescriptor allows 'this' to avoid implementing
     * a dummy getValueProperty.
     */
    template <class ParentImp>
    inline bool getStaticFunctionDescriptor(ExecState* exec, const HashTable* table, JSObject* thisObj, PropertyName propertyName, PropertyDescriptor& descriptor)
    {
        if (ParentImp::getOwnPropertyDescriptor(static_cast<ParentImp*>(thisObj), exec, propertyName, descriptor))
            return true;
        
        const HashEntry* entry = table->entry(exec, propertyName);
        if (!entry)
            return false;
        
        PropertySlot slot;
        bool present = setUpStaticFunctionSlot(exec, entry, thisObj, propertyName, slot);
        if (present)
            descriptor.setDescriptor(slot.getValue(exec, propertyName), entry->attributes());
        return present;
    }

    /**
     * Simplified version of getStaticPropertySlot in case there are no functions, only "values".
     * Using this instead of getStaticPropertySlot removes the need for a FuncImp class.
     */
    template <class ThisImp, class ParentImp>
    inline bool getStaticValueSlot(ExecState* exec, const HashTable* table, ThisImp* thisObj, PropertyName propertyName, PropertySlot& slot)
    {
        const HashEntry* entry = table->entry(exec, propertyName);

        if (!entry) // not found, forward to parent
            return ParentImp::getOwnPropertySlot(thisObj, exec, propertyName, slot);

        ASSERT(!(entry->attributes() & Function));

        slot.setCacheableCustom(thisObj, entry->propertyGetter());
        return true;
    }

    /**
     * Simplified version of getStaticPropertyDescriptor in case there are no functions, only "values".
     * Using this instead of getStaticPropertyDescriptor removes the need for a FuncImp class.
     */
    template <class ThisImp, class ParentImp>
    inline bool getStaticValueDescriptor(ExecState* exec, const HashTable* table, ThisImp* thisObj, PropertyName propertyName, PropertyDescriptor& descriptor)
    {
        const HashEntry* entry = table->entry(exec, propertyName);
        
        if (!entry) // not found, forward to parent
            return ParentImp::getOwnPropertyDescriptor(thisObj, exec, propertyName, descriptor);
        
        ASSERT(!(entry->attributes() & Function));
        PropertySlot slot;
        slot.setCustom(thisObj, entry->propertyGetter());
        descriptor.setDescriptor(slot.getValue(exec, propertyName), entry->attributes());
        return true;
    }

    template <class ThisImp>
    inline void putEntry(ExecState* exec, const HashEntry* entry, PropertyName propertyName, JSValue value, ThisImp* thisObj, bool shouldThrow = false)
    {
        // If this is a function put it as an override property.
        if (entry->attributes() & Function)
            thisObj->putDirect(exec->vm(), propertyName, value);
        else if (!(entry->attributes() & ReadOnly))
            entry->propertyPutter()(exec, thisObj, value);
        else if (shouldThrow)
            throwTypeError(exec, StrictModeReadonlyPropertyWriteError);
    }

    /**
     * This one is for "put".
     * It looks up a hash entry for the property to be set.  If an entry
     * is found it sets the value and returns true, else it returns false.
     */
    template <class ThisImp>
    inline bool lookupPut(ExecState* exec, PropertyName propertyName, JSValue value, const HashTable* table, ThisImp* thisObj, bool shouldThrow = false)
    {
        const HashEntry* entry = table->entry(exec, propertyName);
        
        if (!entry)
            return false;

        putEntry<ThisImp>(exec, entry, propertyName, value, thisObj, shouldThrow);
        return true;
    }

    /**
     * This one is for "put".
     * It calls lookupPut<ThisImp>() to set the value.  If that call
     * returns false (meaning no entry in the hash table was found),
     * then it calls put() on the ParentImp class.
     */
    template <class ThisImp, class ParentImp>
    inline void lookupPut(ExecState* exec, PropertyName propertyName, JSValue value, const HashTable* table, ThisImp* thisObj, PutPropertySlot& slot)
    {
        if (!lookupPut<ThisImp>(exec, propertyName, value, table, thisObj, slot.isStrictMode()))
            ParentImp::put(thisObj, exec, propertyName, value, slot); // not found: forward to parent
    }

} // namespace JSC

#endif // Lookup_h
