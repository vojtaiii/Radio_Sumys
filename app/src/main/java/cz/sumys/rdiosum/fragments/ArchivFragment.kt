package cz.sumys.rdiosum.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import cz.sumys.rdiosum.R
import cz.sumys.rdiosum.databinding.FragmentArchivBinding


class ArchivFragment : Fragment() {
    private lateinit var binding: FragmentArchivBinding

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // binding
        binding = DataBindingUtil.inflate(inflater,
            R.layout.fragment_archiv, container, false)
        

        return binding.root
    }


}