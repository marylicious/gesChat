package com.example.geschat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

import com.example.geschat.models.Announcement;
import com.example.geschat.models.Chat;
import com.example.geschat.adapters.ChatAdapter;
import com.example.geschat.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;


public class ChatFragment extends Fragment implements ChatAdapter.OnChatListListener{

    ArrayList<Chat> chats;
    DatabaseReference chatRef,db;
    RecyclerView rvChats;
    TextView textView;
    String role;
    FloatingActionButton floatingActionButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container,false);
        setHasOptionsMenu(true);

        //para abrir el add chat


        FloatingActionButton floatingActionButton2 = view.findViewById(R.id.btn_refresh);

        floatingActionButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                update();
                Toast.makeText(getContext(), "Chat List refreshed", Toast.LENGTH_SHORT).show();
            }
        });

        floatingActionButton = (FloatingActionButton) view.findViewById(R.id.btn_addChat);

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent in = new Intent(getActivity(), AddChatActivity.class);
                startActivity(in);
            }
        });

        floatingActionButton.hide();
        db = FirebaseDatabase.getInstance().getReference();



        LinearLayout ln = view.findViewById(R.id.sortByStatusBtn);
        ln.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Chats Sorted by Status", Toast.LENGTH_SHORT).show();
                Collections.sort(chats,Chat.ByStatus);
                update();
            }
        });

         ln = view.findViewById(R.id.sortByDateBtn);
        ln.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Chats Sorted by Users", Toast.LENGTH_SHORT).show();
                Collections.sort(chats,Chat.ByUsers);
                update();
            }
        });


        rvChats = view.findViewById(R.id.chatView);
        rvChats.setLayoutManager(new LinearLayoutManager(getActivity()));

        chatRef = FirebaseDatabase.getInstance().getReference().child("Chat");

        //Load from DB
        chatRef.addValueEventListener(new ValueEventListener() {



            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chats = new ArrayList<>();

                if(dataSnapshot.exists()){

                for (DataSnapshot dataSnapshotChat : dataSnapshot.getChildren()) {
                    Boolean fin = (Boolean) dataSnapshotChat.child("finished").getValue();
                    Boolean aprov = (Boolean) dataSnapshotChat.child("approvedProposal").getValue();

                    if(!fin && aprov) {

                        ArrayList<String> assistanceListKeys = new ArrayList<>();
                        Boolean approvedProposal = dataSnapshotChat.child("approvedProposal").getValue(Boolean.class);
                        String dateEpoch = dataSnapshotChat.child("date").getValue(String.class);
                        String facilitator = dataSnapshotChat.child("facilitator").getValue(String.class);
                        String comments = dataSnapshotChat.child("comments").getValue(String.class);
                        Boolean finished = dataSnapshotChat.child("finished").getValue(Boolean.class);
                        Boolean isFilled = dataSnapshotChat.child("filled").getValue(Boolean.class);
                        String presentation = dataSnapshotChat.child("presentation").getValue(String.class);
                        String chatName = dataSnapshotChat.child("title").getValue(String.class);
                        String level = dataSnapshotChat.child("level").getValue(String.class);
                        String startHour = dataSnapshotChat.child("startHour").getValue(String.class);
                        String endHour = dataSnapshotChat.child("endHour").getValue(String.class);

                        int amountPeople;

                        //este query no va aqui pero lo deje para usarlo cuando necesite la lista de asistencia

                        if (dataSnapshotChat.hasChild("assistanceList")) {


                            for (DataSnapshot userUID : dataSnapshotChat.child("assistanceList").getChildren()) {
                                String userKey = userUID.getValue(String.class);
                                assistanceListKeys.add(userKey);
                            }

                            amountPeople = (int) dataSnapshotChat.child("assistanceList").getChildrenCount();


                        } else {
                            amountPeople = 0;
                        }


                        final Chat chat = new Chat(assistanceListKeys, approvedProposal, dateEpoch, facilitator, comments, finished, isFilled, presentation, chatName, level, amountPeople, startHour, endHour);
                        chat.setKeyDB(dataSnapshotChat.getKey());


                        FirebaseDatabase.getInstance().getReference().child("Users").child(facilitator).child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                chat.setFacilitatorName(dataSnapshot.getValue(String.class));

                                if(!chat.getFinished() && chat.getProposalApproved()){
                                    chats.add(chat);
                                }

                                setChatAdapter();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                } // here

            } else {
                    setChatAdapter();
                }

            } //here

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "There was an error fetching chats", Toast.LENGTH_SHORT).show();
            }
        });


        fetchRole();

        return view;
    }


    @Override
    public void onChatClick(int position){

        //Obtengo el chat que estoy tocando
        Chat chat = chats.get(position);

        //Navegaremos a nueva Activity cuando el user toque
        Intent intent = new Intent(getActivity(), ChatActivity.class);



        intent.putExtra("chatname", chat.getChatName());
        intent.putExtra("level", chat.getLevel() );
        intent.putExtra("facilitatorName", chat.getFacilitatorName());
        intent.putExtra("date", chat.getDate());
        intent.putExtra("facilitator", chat.getFacilitator());
        intent.putExtra("startHour", chat.getStartTime());
        intent.putExtra("endHour",chat.getEndTime());
        intent.putExtra("amountPeople", Integer.toString(chat.getAmountPeople()));
        intent.putExtra("keyDB", chat.getKeyDB());
        intent.putExtra("presentation", chat.getPresentation());
        intent.putExtra("comments", chat.getComments());
        intent.putExtra("filled", chat.getFilled());


        startActivity(intent);



    }

    private void setChatAdapter(){
        Collections.reverse(chats);
        ChatAdapter adapter = new ChatAdapter(chats,this);
        rvChats.setAdapter(adapter);
    }


    public void update(){
        ChatAdapter adapter = new ChatAdapter(chats,this);
        rvChats.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void setVisibleToSupervisor(){
        floatingActionButton.show();

    }

    private void fetchRole(){
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.child("Users").child(uid).child("role").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                role = dataSnapshot.getValue(String.class);
                if(role.equals("supervisor") || role.equals("facilitator")){
                    setVisibleToSupervisor();
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {
            }
        });


    }


}
